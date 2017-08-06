@file:JvmName("DownloadInTamil")
package com.saaral

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.UnexpectedPage
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.*
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.fusesource.jansi.Ansi
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.logging.Logger.getLogger
import java.util.ArrayList
import java.util.concurrent.TimeUnit


//Globals , Visible to this IDEA module
internal var proxyHostName = ""
internal var proxyPort =0


fun canProceed() : Boolean {
    var goAhead : Boolean? = null
    val console=System.console()
    var userInput = "N"
    while (goAhead == null) {
        println(Ansi.ansi().fg(Ansi.Color.GREEN).a("INFO: Press 'Y' to download songs, 'N' to exit: ").reset())
        try {
            userInput = console.readLine() ?: "N"
        }
        catch (ex: Exception){
            //If Can not read from Console , Make it as a false
            println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: Can not read from Console "+ex.message).reset())
            userInput = "N"
            ex.printStackTrace()
        }
        when {
            Regex("(?i)[N]").matches(userInput) -> goAhead = false
            Regex("(?i)[Y]").matches(userInput) -> goAhead = true
        }
    }
    return goAhead
}

fun downloadSingleSongFromURL(downloadDirectory: String,songFN:String,songURL:String) : Boolean {

    var songID = -1.toLong()
    var songFN = songFN //Fix file name
    songFN = songFN.replace("(\\d+)\\.\\s*".toRegex(),"") //Remove numbers
    songFN = songFN.replace("\\s+".toRegex(),"_") //Replace space to _
    songFN = songFN.replace("_$".toRegex(),"") //Last _ to none

    println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Start search $songFN info from $songURL into $downloadDirectory ...").reset())

    //Find download link from the song link
    //intamil logic http://intamil.co/song/<NUMBER>/Dooram-Nillu => http://intamil.co/download/<NUMBER> is download link
    val songIDRegExPattern = "(?i)\\s*http[s]?://.*/song/(.*)/.*".toRegex()
    songID = songIDRegExPattern.matchEntire(songURL)?.groups?.get(1)?.value?.toLong() ?: -1.toLong()
    if (songID ==-1.toLong()){
        println(Ansi.ansi().fg(Ansi.Color.RED).a("INFO: Can not find intamil songid from URL $songURL ...").reset())
        return false
    }
    val songDownloadInTamilURL="http://intamil.co/download/"+songID.toString()
    println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Downloading $songFN from URL $songDownloadInTamilURL ...").reset())
    try {

        val browser = openBrowser()
        val songfilePage = browser.getPage<UnexpectedPage>(songDownloadInTamilURL)
        if (!songfilePage.webResponse.contentType.startsWith("audio/", true)) {
            println(Ansi.ansi().fg(Ansi.Color.RED).a("WARNING:: Content type does not look like song. its " + songfilePage.webResponse.contentType).reset())
        }

        var inpStream = songfilePage.webResponse.contentAsStream
        var bytescopied = inpStream.copyTo(File("$downloadDirectory/$songFN").outputStream())

        if (bytescopied == 0.toLong()) {
            println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: Can not copy Input Stream Bytes to File $downloadDirectory/$songFN"))
            return false
        }
        //println(FilenameUtils.getName(songfilePage.url.file) + ","+songfilePage.url.file)
    }
    catch (ex: Exception){

        println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: Downloading $songFN from $songDownloadInTamilURL"+ex.message))
        ex.printStackTrace()
        return false
    }

    println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Done Downloading $songFN from $songURL into $downloadDirectory ").reset())
    return true
}

fun downloadSongs(downloadDirectory: String, songsNameAndURL:LinkedHashMap<String,String>) : Int  {
    var numberofSongsDownloaded = 0
    val numberofProcessors =  Runtime.getRuntime().availableProcessors() - 1  // Leave one for the main processing
    println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Create download dir $downloadDirectory").reset())
    try {
        FileUtils.forceMkdir(File(downloadDirectory))
    }
    catch (ex: Exception){
        println("Can not create directory $downloadDirectory. Stop")
        println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: "+ex.message))
        ex.printStackTrace()
        System.exit(4)
    }

    println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Starting $numberofProcessors download process in parallel").reset())
    val jobExecutor = Executors.newFixedThreadPool(numberofProcessors)
    val downloadJobsList = arrayListOf<Callable<Boolean>>()
    //unpack map and dispatch the Jobs
    for ((songURL, songName) in songsNameAndURL) {
        //Callable<Boolean>({downloadSingleSongFromURL(downloadDirectory, songName, songURL)}) , Create Callable Object with Lambda / closure as parameter, () optional
        val callableTask = Callable<Boolean>{
            downloadSingleSongFromURL(downloadDirectory, songName, songURL)
        }
        downloadJobsList.add(callableTask)
    }
    //Start and Wait for all the Jobs to complete.
    //Note: Here ! means its a platform type, Kotlin can not know its a null or not
    try {
        val downloadStatusFutureList = jobExecutor.invokeAll(downloadJobsList)

        //Get the Download status results. Future get is a blocking call
        downloadStatusFutureList.forEach {
            val downloadStatus = it.get() ?: false
            if (downloadStatus) numberofSongsDownloaded++
        }

        //shutdown pool
        jobExecutor.shutdown()
        if (!jobExecutor.awaitTermination(2, TimeUnit.HOURS)) { //Shutdown forcefully after 2 Hrs
            jobExecutor.shutdownNow()
        }
    }
    catch (ex: Exception){
        jobExecutor.shutdownNow()
        println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: "+ex.message))
        ex.printStackTrace()
    }

    println("INFO: $numberofSongsDownloaded files downloaded in $downloadDirectory")
    return numberofSongsDownloaded

}
/**
 * This function sets the proxyHostName , proxyPort module level functions. Call this function only once in main
 * @param none
 * @return none
 */
fun setHTTPProxy(){

    val envProxy = System.getenv("http_proxy") ?: ""
    val envProxyPatternRegEx = "(?i)\\s*http[s]?://(.*):(\\d+)".toRegex()
    proxyHostName = envProxyPatternRegEx.matchEntire(envProxy)?.groups?.get(1)?.value ?: ""
    proxyPort = envProxyPatternRegEx.matchEntire(envProxy)?.groups?.get(2)?.value?.toInt() ?: 0
    if (proxyHostName != "" && proxyPort !=0){
        println(Ansi.ansi().fg(Ansi.Color.YELLOW).a("INFO: Using Environment http_proxy variable").reset())
        println(Ansi.ansi().fg(Ansi.Color.YELLOW).a("INFO: Proxy Host: $proxyHostName , Port: $proxyPort").reset())
    }
    else{
        println(Ansi.ansi().fg(Ansi.Color.YELLOW).a("INFO: No http_proxy variable found. Assuming direct internet connection").reset())
    }

}

fun openBrowser() : WebClient {

    val browser = if (proxyHostName != "" && proxyPort !=0){
        WebClient(BrowserVersion.CHROME,proxyHostName,proxyPort)
    }
    else{
        WebClient(BrowserVersion.CHROME)
    }
    browser.options.apply {
        isUseInsecureSSL = true
        isJavaScriptEnabled = true
        isCssEnabled = false
        isThrowExceptionOnScriptError = false
        isDoNotTrackEnabled = true
        isActiveXNative = false
        isAppletEnabled = false
        isRedirectEnabled = true
        isDownloadImages = false
        isGeolocationEnabled = false
        isPrintContentOnFailingStatusCode = true
        timeout=200000
    }
    browser.waitForBackgroundJavaScript(200000)

    return browser
}

fun main(vararg  args: String){

    getLogger("com.gargoylesoftware").level = Level.OFF

    val movieURL = "http://intamil.co/songs/Velaiilla-Pattadhari-2"
    val songsDownloadBaseDir = "/tmp"
    val songsMap = linkedMapOf<String,String>()
    //Download the HTML page of the intamil movie
    //Initialize the browser
    setHTTPProxy()
    val browser = openBrowser()

    try {
        println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Downloading data from $movieURL . Wait...").reset())
        val moviePage = browser.getPage<HtmlPage>(movieURL)
        //println(moviePage.asText())
        //println(moviePage.asXml())
        val movieName = moviePage.getByXPath<HtmlFigureCaption>("//div/figure/figcaption")?.firstOrNull()?.asText() ?: "!Unknown!"
        val directorName = moviePage.getByXPath<HtmlSpan>("//span[contains(@itemprop , 'director')]")?.firstOrNull()?.asText() ?: "!Unknown!"
        val musicDirectorName = moviePage.getByXPath<HtmlSpan>("//span[contains(@itemprop,'musicBy')]")?.firstOrNull()?.asText() ?: "!Unknown!"
        val castName = moviePage.getByXPath<HtmlSpan>("//span[contains(@itemprop,'actor')]")?.firstOrNull()?.asText() ?: "!Unknown!"

        val songsHtmlDivList = moviePage.getByXPath<HtmlDivision>("//div[contains(@class,'song_list_title')]")

        println(Ansi.ansi().fg(Ansi.Color.GREEN).a("INFO: Movie: "+movieName).reset())
        println(Ansi.ansi().fg(Ansi.Color.GREEN).a("INFO: Director: "+directorName).reset())
        println(Ansi.ansi().fg(Ansi.Color.GREEN).a("INFO: Music Director: "+musicDirectorName).reset())
        println(Ansi.ansi().fg(Ansi.Color.GREEN).a("INFO: Cast: "+castName).reset())
        println(Ansi.ansi().fg(Ansi.Color.MAGENTA).a("INFO: ------ Songs -------- "))
        songsHtmlDivList.forEach {
            //val songName = it.getByXPath<HtmlAnchor>("a").firstOrNull()?.asText() ?: "Unknown"
            val songName = it.asText() ?: "!Unknown!"
            val songURL = it.getByXPath<HtmlAnchor>("a").firstOrNull()?.hrefAttribute ?: "!Unknown!"
            if (songName !="!Unknown!" &&  songURL !="!Unknown!" ) {
                songsMap[songURL]=songName
                println("\t" + songName)
            }

        }
        println(Ansi.ansi().fg(Ansi.Color.MAGENTA).a("INFO: ------ Songs -------- ").reset())

        //TODO: Enebale later if (!canProceed()) System.exit(5)

        //Songs details are ready, Download
        downloadSongs(songsDownloadBaseDir+"/"+movieName,songsMap)

        //TODO: Test purpose
        //downloadSingleSongFromURL("/tmp/vip2","1. Dooram Nillu.mp3","http://intamil.co/song/2430/Dooram-Nillu")
    }
    catch (ex:Exception){
        println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: "+ex.message))
        ex.printStackTrace()
    }


}