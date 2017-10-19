@file:JvmName("DownloadInTamil")

package com.saaral

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException
import com.gargoylesoftware.htmlunit.UnexpectedPage
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.*
import org.apache.commons.cli.*
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.builder.RecursiveToStringStyle
import org.apache.commons.lang3.builder.ReflectionToStringBuilder
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger.getLogger
import kotlin.system.exitProcess

data class IntamilHostDetails(
        val intamilHostName: String = "",
        val intamilProto: String = "",
        val intamilMovieSearchSiteBaseForMovieNameStartsWithNumbers: String = "",
        val intamilMovieSearchSiteBaseForMovieNameStartsWithAlphabets: String = ""
)

data class ProxyDetails(
        val proxyHostName: String = "",
        val proxyPort: Int = 0
)

//Globals , Visible to this IDEA module
internal var bypassProxy = false
internal val proxyDetailsDataObj: ProxyDetails by lazy { setHTTPProxy() }
internal val intamilHostDetailsDataObj: IntamilHostDetails by lazy { findInTamilHostDetails() }
internal val NEW_LINE = System.lineSeparator() ?: "\n"
internal val browserObj: WebClient by lazy { createBrowser() }

fun createBrowser(): WebClient {
    val browser = if (!bypassProxy && proxyDetailsDataObj.proxyHostName != "" && proxyDetailsDataObj.proxyPort != 0) {
        WebClient(BrowserVersion.FIREFOX_52, proxyDetailsDataObj.proxyHostName, proxyDetailsDataObj.proxyPort)
    } else {
        WebClient(BrowserVersion.FIREFOX_52)
    }
    browser.options.apply {
        isUseInsecureSSL = true
        isJavaScriptEnabled = false // Disable for faster execution
        isCssEnabled = false
        isThrowExceptionOnScriptError = false
        isDoNotTrackEnabled = true
        isActiveXNative = false
        isAppletEnabled = false
        isRedirectEnabled = true
        isDownloadImages = false
        isGeolocationEnabled = false
        isPrintContentOnFailingStatusCode = true
        timeout = 600000 //In Milli seconds, 10 minutes

    }
    browser.waitForBackgroundJavaScript(240000) // 4Mins

    return browser
}

/**
 * This function sets the proxyHostName , proxyPort module level functions. Call this function only once in main
 * @param
 * @return
 */
fun setHTTPProxy(): ProxyDetails {
    if (bypassProxy) return ProxyDetails()

    val envProxy = System.getenv("http_proxy") ?: ""
    val envProxyPatternRegEx = "(?i)\\s*http[s]?://(.*):(\\d+)".toRegex()

    val proxyHostName = envProxyPatternRegEx.matchEntire(envProxy)?.groups?.get(1)?.value ?: ""
    val proxyPort = envProxyPatternRegEx.matchEntire(envProxy)?.groups?.get(2)?.value?.toInt() ?: 0

    return if (proxyHostName != "" && proxyPort != 0) {
        println(Ansi.ansi().fg(Ansi.Color.YELLOW).a("INFO: Using Environment http_proxy variable").reset())
        println(Ansi.ansi().fg(Ansi.Color.YELLOW).a("INFO: Proxy Host: $proxyHostName , Port: $proxyPort").reset())
        ProxyDetails(proxyHostName, proxyPort)
    } else {
        println(Ansi.ansi().fg(Ansi.Color.YELLOW).a("INFO: No http_proxy variable found. Assuming direct internet connection").reset())
        ProxyDetails()
    }
}

private fun findInTamilHostDetails(): IntamilHostDetails {

    val intamilProto = "http"
    val intamilhostlist = arrayListOf("intamil.in", "intamil.co")

    intamilhostlist.forEach {
        try {
            browserObj.getPage<HtmlPage>("$intamilProto://$it")
            println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: $intamilProto://$it is alive.").reset())
            return IntamilHostDetails(it, intamilProto, "$intamilProto://$it/movie-alphabet/0-9", "$intamilProto://$it/movie-alphabet")

        } catch (ex: FailingHttpStatusCodeException) {
            //Can not open this URL, Give warning and try again
            println(Ansi.ansi().fg(Ansi.Color.CYAN).a("WARNING: $intamilProto://$it is not accessible, trying other mirrors.").reset())
        } catch (ex: Exception) {
            println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: " + ex.message).reset())
            throw ex
        }
    }
    throw RuntimeException("All the intamil mirrors are not reachable. Can not continue")
}

fun defineCmdLineOptions(): Options {
    return Options().apply {

        addOption(with(Option.builder()) {
            argName("intamil_Movie_Name")
            desc("Tamil Movie Name to search in ${intamilHostDetailsDataObj.intamilHostName} and download songs. Ex: vivegam . [Optional when --url option is provided.]")
            longOpt("search")
            numberOfArgs(1)
            required(false)
            type(String::class.java) //Refer PatternOptionBuilder doc for types
            build()
        })

        addOption(with(Option.builder()) {
            argName("intamil_movie_URL")
            desc("${intamilHostDetailsDataObj.intamilHostName} movie URL to download songs. Ex: http://${intamilHostDetailsDataObj.intamilHostName}/songs/Mersal . [Optional when --search option is provided.]")
            longOpt("url")
            numberOfArgs(1)
            required(false)
            type(URL::class.java) //Refer PatternOptionBuilder doc for types
            build()
        })

        addOption(with(Option.builder()) {
            argName("sync_With_InTamil")
            desc("Download \'Latest Tamil Songs\' section movie songs in ${intamilHostDetailsDataObj.intamilHostName} only if the movie folder not already present in --outdir")
            longOpt("sync")
            numberOfArgs(0)
            required(false)
            build()
        })

        addOption(with(Option.builder()) {
            argName("Output_Directory")
            desc("Output directory where movie specific folders will get created to download songs. Ex:C:/Temp")
            longOpt("outdir")
            numberOfArgs(1)
            required(true)
            type(File::class.java) //Refer PatternOptionBuilder doc for types
            build()
        })

        addOption(with(Option.builder("h")) {
            argName("help")
            desc("Shows Help message")
            longOpt("help")
            optionalArg(false)
            numberOfArgs(0)
            required(false)
            build()
        })

        addOption(with(Option.builder()) {
            argName("No Proxy")
            desc("Dont honor System Proxy Environment Variables (http_proxy, https_proxy)")
            longOpt("no-proxy")
            optionalArg(false)
            numberOfArgs(0)
            required(false)
            build()
        })
    }
}

fun showHelp(cmdLineOptionsTemplate: Options, myJarFileName: String, myJarVersion: String, myJarFullPath: String) {
    val header = "$myJarFileName v$myJarVersion is a tool to download all the movie songs from ${intamilHostDetailsDataObj.intamilProto}://${intamilHostDetailsDataObj.intamilHostName} website.$NEW_LINE" +
            " ${intamilHostDetailsDataObj.intamilHostName} website do not have option to download all the songs of a movie at one go. This tool download all songs in parallel. This tool also can search movie from intamil website."
    val footer = "(c) Bakkiaraj Murugesan $NEW_LINE https://bakkiaraj.github.io/DownloadInTamil $NEW_LINE License: MIT License $NEW_LINE Local Install: $myJarFullPath"

    HelpFormatter().apply {
        //descPadding = 10
        //leftPadding = 20
        width = 180
        printHelp(Ansi.ansi().fg(Ansi.Color.GREEN).a("java -jar $myJarFileName [Options]").toString(),
                Ansi.ansi().fg(Ansi.Color.DEFAULT).a("$NEW_LINE $header $NEW_LINE $NEW_LINE[Options]").toString(),
                cmdLineOptionsTemplate,
                Ansi.ansi().fg(Ansi.Color.CYAN).a("$NEW_LINE $footer $NEW_LINE").reset().toString())
    }
    exitProcess(2)
}

/*fun canProceed() : Boolean {
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
}*/

fun downloadSingleSongFromURL(downloadDirectory: String, songFileName: String, songURL: String): Boolean {

    var songFN = songFileName //Fix file name
    songFN = songFN.replace("(\\d+)\\.\\s*".toRegex(), "") //Remove numbers
    songFN = songFN.replace("\\s+".toRegex(), "_") //Replace space to _
    songFN = songFN.replace("_$".toRegex(), "") //Last _ to none

    println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Start search $songFN info from $songURL ...").reset())

    //Find download link from the song link
    //intamil logic http://intamil.co/song/<NUMBER>/Dooram-Nillu => http://intamil.co/download/<NUMBER> is download link
    val songIDRegExPattern = "(?i)\\s*http[s]?://.*/song/(.*)/.*".toRegex()
    val songID = songIDRegExPattern.matchEntire(songURL)?.groups?.get(1)?.value?.toLong() ?: (-1).toLong()
    if (songID == (-1).toLong()) {
        println(Ansi.ansi().fg(Ansi.Color.RED).a("INFO: Can not find intamil songid from URL $songURL into $downloadDirectory ...").reset())
        return false
    }
    val songDownloadInTamilURL = intamilHostDetailsDataObj.intamilProto + "://" + intamilHostDetailsDataObj.intamilHostName + "/download/" + songID.toString()
    println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Downloading $songFN from URL $songDownloadInTamilURL ...").reset())
    try {
        val songfilePage = browserObj.getPage<UnexpectedPage>(songDownloadInTamilURL)
        if (!songfilePage.webResponse.contentType.startsWith("audio/", true)) {
            println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: Content type does not look like song. its " + songfilePage.webResponse.contentType).reset())
        }

        val inpStream = songfilePage.webResponse.contentAsStream
        val bytescopied = inpStream.copyTo(File("$downloadDirectory/$songFN").outputStream())

        if (bytescopied == 0.toLong()) {
            println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: Can not copy Input Stream Bytes to File $downloadDirectory/$songFN"))
            return false
        }
        //println(FilenameUtils.getName(songfilePage.url.file) + ","+songfilePage.url.file)
    } catch (ex: Exception) {

        println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: Downloading $songFN from $songDownloadInTamilURL" + ex.message).reset())
        System.out.flush()
        System.err.flush()
        ex.printStackTrace()
        return false
    }

    println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Done Downloading $songFN from $songURL into $downloadDirectory ").reset())
    return true
}

fun downloadMovieSongsInParallel(downloadDirectory: String, songsNameAndURL: LinkedHashMap<String, String>) {
    var numberofSongsDownloaded = 0
    val numberofProcessors = Runtime.getRuntime().availableProcessors() - 1  // Leave one for the main processing

    println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Create download dir $downloadDirectory").reset())
    try {
        FileUtils.forceMkdir(File(downloadDirectory))
    } catch (ex: Exception) {
        println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: Can not create directory $downloadDirectory. Return" + ex.message).reset())
        System.out.flush()
        System.err.flush()
        ex.printStackTrace()
        return
    }

    println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Starting $numberofProcessors download process in parallel").reset())
    val jobExecutor = Executors.newFixedThreadPool(numberofProcessors)
    val downloadJobsList = arrayListOf<Callable<Boolean>>()
    //unpack map and dispatch the Jobs
    for ((songURL, songName) in songsNameAndURL) {
        //Callable<Boolean>({downloadSingleSongFromURL(downloadDirectory, songName, songURL)}) , Create Callable Object with Lambda / closure as parameter, () optional
        val callableTask = Callable<Boolean> {
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
            if (it.get()) numberofSongsDownloaded++
        }

        //shutdown pool
        jobExecutor.shutdown()
        if (!jobExecutor.awaitTermination(2, TimeUnit.HOURS)) { //Shutdown forcefully after 2 Hrs
            jobExecutor.shutdownNow()
        }
    } catch (ex: Exception) {
        jobExecutor.shutdownNow()
        println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: " + ex.message).reset())
        throw ex
    }

    println("INFO: $numberofSongsDownloaded files downloaded in $downloadDirectory")
    return
}

fun getJavaVersion(): Double {
    val version = System.getProperty("java.version") ?: "0.0.0"
    var pos = version.indexOf('.')
    pos = version.indexOf('.', pos + 1)

    return (version.substring(0, pos)).toDouble()
}

private fun askUserInputNumber(maxCount: Int): Int {
    println("")
    val console = Scanner(System.`in`)
    while (true) {
        try {
            println(Ansi.ansi().fg(Ansi.Color.YELLOW).a("ASK: Enter number from 1 to $maxCount : ").reset())
            val userInput = console.nextInt()
            if (userInput in 1..maxCount) return userInput
        } catch (ex: InputMismatchException) {
            println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: Wakeup. Enter only numbers and press enter.").reset())
            console.nextLine()
        } catch (ex: Exception) {
            //If Can not read from Console , Make it as a false
            println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: Can not read from Console " + ex.message).reset())
            console.nextLine()
            throw ex
        }
    }
}

fun searchAndFindMovieURL(movieSearchString: String): List<URL> {
    //Sanitize user input
    var searchString = movieSearchString.replace("""\r?\n""".toRegex(), "") //Remove new lines
    searchString = searchString.replace("""^\s+""".toRegex(), "") //Remove Leading Spaces
    searchString = searchString.replace("""\s+$""".toRegex(), "") //Remove trailing Spaces

    //Remove quotes
    searchString = searchString.replace("""^'""".toRegex(), "")
    searchString = searchString.replace("""'$""".toRegex(), "")
    searchString = searchString.replace("""^"""".toRegex(), "")
    searchString = searchString.replace(""""$""".toRegex(), "")

    println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Searching Movie Name contains [$searchString] in intamil website").reset())
    //Set the search base URL
    //println("*****" + searchString)
    val searchBaseURL = if (searchString.matches("""^\w+.*""".toRegex())) {
        URL(intamilHostDetailsDataObj.intamilMovieSearchSiteBaseForMovieNameStartsWithAlphabets + "/" + searchString[0].toLowerCase())
    } else {
        URL(intamilHostDetailsDataObj.intamilMovieSearchSiteBaseForMovieNameStartsWithNumbers)
    }

    val selectedMovieName: String
    val selectedMovieURL: String // The var should be initialized before first use. in such case, type is enough.

    val movieNameAndURLIndexSortedMap = sortedMapOf<String, String>()
    val movieNameRegEx = """(?i)$searchString""".toRegex()

    try {
        //Get the index URL
        println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Downloading Movie Index from " + searchBaseURL.toString() + " Wait...").reset())
        val movieIndexPage = browserObj.getPage<HtmlPage>(searchBaseURL)
        val moviesHtmlDivList = movieIndexPage.getByXPath<HtmlDivision>("//div[contains(@class,'movie_icon')]")
        println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Done Downloaded Movie Index from " + searchBaseURL.toString()).reset())

        //Processing the movies
        moviesHtmlDivList.forEach {
            val movieURL = it.getByXPath<HtmlAnchor>("./a").firstOrNull()?.hrefAttribute ?: "!Unknown!"
            val movieName = it.getByXPath<DomText>(".//strong/text()").firstOrNull()?.data ?: "!Unknown!"
            if (movieURL != "!Unknown!" && movieName != "!Unknown!") {
                //Look for matched moviename from index
                if (movieNameRegEx.containsMatchIn(movieName)) {
                    movieNameAndURLIndexSortedMap[movieName] = movieURL
                }
            }

        }
    } catch (ex: Exception) {
        println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: " + ex.message).reset())
        throw ex
    }
    // If not found
    if (movieNameAndURLIndexSortedMap.isEmpty()) {
        println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: Can not find $searchString movie in $searchBaseURL using regex " + movieNameRegEx.toString()).reset())
        println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: May be movie songs are not yet uploaded. Can not continue. Quit.").reset())
        throw IllegalStateException("Can not find movie")
    }
    //If more items are found
    if (movieNameAndURLIndexSortedMap.count() > 1) {
        println("")
        println(Ansi.ansi().fg(Ansi.Color.YELLOW).a("INFO: Multiple Movies are found. You need to *Select*").reset())
        var item = 0
        val matchedMoviesList = movieNameAndURLIndexSortedMap.keys.toList()
        matchedMoviesList.forEach {
            item++
            println(Ansi.ansi().fg(Ansi.Color.YELLOW).a("[$item] $it").reset())
        }
        val userInput = askUserInputNumber(item)
        selectedMovieName = matchedMoviesList[userInput - 1] ?: "!Unknown!" //Because index start from 0
        selectedMovieURL = movieNameAndURLIndexSortedMap[selectedMovieName] ?: "!Unknown!"
    } else {
        //Single item
        selectedMovieName = movieNameAndURLIndexSortedMap.firstKey() ?: "!Unknown!"
        selectedMovieURL = movieNameAndURLIndexSortedMap[selectedMovieName] ?: "!Unknown!"
    }

    return if (selectedMovieName != "!Unknown!" && selectedMovieURL != "!Unknown!") {
        println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Found Movie Name: $selectedMovieName , in URL: $selectedMovieURL").reset())
        listOf(URL(selectedMovieURL))
    } else {
        throw IllegalStateException("Can not retrieve selected movie name and url. Internal Error")
    }
}

fun dump(obj: Any) {
    println("DUMP:--")
    println(ReflectionToStringBuilder.toString(obj, RecursiveToStringStyle.MULTI_LINE_STYLE, true, true))
    println("DUMP:--")
}

fun downloadAllSongsInMovie(movieURL: URL, songsDownloadBaseDir: File) {
    if (!movieURL.host.startsWith("intamil")) {
        println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: $movieURL does not seems to be intamil website like ${intamilHostDetailsDataObj.intamilProto}://${intamilHostDetailsDataObj.intamilHostName} etc... , At this time, we support only intamil website."))
        throw IllegalArgumentException("Movie URL does not seems to be intamil website.")
    }
    if (!songsDownloadBaseDir.exists() || !songsDownloadBaseDir.isDirectory || !songsDownloadBaseDir.canWrite()) {
        println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: $songsDownloadBaseDir does not exists OR its not a writable directory / folder.").reset())
        throw IllegalAccessError("Output Directory Access Error.")
    }

    val songsMap = linkedMapOf<String, String>()
    //Download the HTML page of the intamil movie
    println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Downloading data from $movieURL . Wait...").reset())
    val moviePage = browserObj.getPage<HtmlPage>(movieURL)
    //println(moviePage.asText())
    //println(moviePage.asXml())
    val movieName = moviePage.getByXPath<HtmlFigureCaption>("//div/figure/figcaption")?.firstOrNull()?.asText() ?: "!Unknown!"
    val directorName = moviePage.getByXPath<HtmlSpan>("//span[contains(@itemprop , 'director')]")?.firstOrNull()?.asText() ?: "!Unknown!"
    val musicDirectorName = moviePage.getByXPath<HtmlSpan>("//span[contains(@itemprop,'musicBy')]")?.firstOrNull()?.asText() ?: "!Unknown!"
    val castName = moviePage.getByXPath<HtmlSpan>("//span[contains(@itemprop,'actor')]")?.firstOrNull()?.asText() ?: "!Unknown!"

    val songsHtmlDivList = moviePage.getByXPath<HtmlDivision>("//div[contains(@class,'song_list_title')]")

    println(Ansi.ansi().fg(Ansi.Color.GREEN).a("INFO: Movie: " + movieName).reset())
    println(Ansi.ansi().fg(Ansi.Color.GREEN).a("INFO: Director: " + directorName).reset())
    println(Ansi.ansi().fg(Ansi.Color.GREEN).a("INFO: Music Director: " + musicDirectorName).reset())
    println(Ansi.ansi().fg(Ansi.Color.GREEN).a("INFO: Cast: " + castName).reset())

    if (songsHtmlDivList.size == 0) {
        println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: Can not find any songs in $movieName from $movieURL. Perhaps songs are not yet uploaded. Can not continue.").reset())
        return
    }

    println(Ansi.ansi().fg(Ansi.Color.MAGENTA).a("INFO: ------ Songs -------- "))
    songsHtmlDivList.forEach {
        val songName = it.asText() ?: "!Unknown!"
        val songURL = it.getByXPath<HtmlAnchor>("./a").firstOrNull()?.hrefAttribute ?: "!Unknown!"
        if (songName != "!Unknown!" && songURL != "!Unknown!") {
            songsMap[songURL] = songName
            println("\t" + songName)
        }

    }
    println(Ansi.ansi().fg(Ansi.Color.MAGENTA).a("INFO: ------ Songs -------- ").reset())

    //Songs details are ready, Download
    downloadMovieSongsInParallel(songsDownloadBaseDir.absolutePath + "/" + movieName, songsMap)

    //Save movie details in file
    File(songsDownloadBaseDir.absolutePath + "/" + movieName + "/info.txt").writeText(
            "Movie: $movieName" + System.lineSeparator() +
                    "Director: $directorName" + System.lineSeparator() +
                    "Music Director: $musicDirectorName" + System.lineSeparator() +
                    "Cast: $castName" + System.lineSeparator()
    )
}

fun downloadLatestMovieURLs(songsDownloadBaseDir: File): List<URL> {
    return listOf<URL>(URL("http://aa.com"), URL("http://bb.com"))
}


fun main(vararg args: String) {

    getLogger("com.gargoylesoftware").level = Level.OFF
    AnsiConsole.systemInstall()

    //Set
    val startTime = System.currentTimeMillis()

    val myDetails = object {
        val myJarFullPath = URLDecoder.decode(this::class.java.protectionDomain.codeSource.location.path, "UTF-8") ?: "."
        val myJarFileName = FilenameUtils.getName(myJarFullPath) ?: "DownloadInTamil"
        val myJarVersion = this::class.java.`package`.implementationVersion ?: "0.0.0"
    }
    println(Ansi.ansi().fg(Ansi.Color.BLUE).a("INFO: Start " + myDetails.myJarFileName + " v" + myDetails.myJarVersion).reset())

    //Check for Java 1.8
    if (getJavaVersion() < 1.8) // We Support only Java 1.8
    {
        println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: Java version 1.8 or latest is needed. Found Java " + getJavaVersion()).reset())
        exitProcess(4)
    }
    println(Ansi.ansi().fg(Ansi.Color.CYAN).a("INFO: Using Java  " + getJavaVersion()).reset())

    //Handle --no-proxy before even parsing the command args due to complxity in finding Hosts
    // Do it before first access to intamilHostDetailsDataObj
    //defineCmdLineOptions() func uses the intamilHostDetailsDataObj
    if ("--no-proxy" in args) bypassProxy = true

    //Handle command line options
    val cmdLineOptionsTemplate = defineCmdLineOptions()
    var cmdLineParser: Any? = null
    try {
        cmdLineParser = DefaultParser().parse(cmdLineOptionsTemplate, args, true)
    } catch (ex: Exception) {
        println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: " + ex.message).reset())
        showHelp(cmdLineOptionsTemplate, myDetails.myJarFileName, myDetails.myJarVersion, myDetails.myJarFullPath)
    }

    //Check for null and it should be CommandLine object. This line enables kotlin smart cast
    //After this "if loop", cmdLineParser is smartCast to CommandLine
    if (cmdLineParser == null || cmdLineParser !is CommandLine) throw RuntimeException("Command Line Options can not be determined.")

    //Check for help
    if (cmdLineParser.hasOption("help")) showHelp(cmdLineOptionsTemplate, myDetails.myJarFileName, myDetails.myJarVersion, myDetails.myJarFullPath)

    //Set the HTTP proxy
    if (cmdLineParser.hasOption("no-proxy")) bypassProxy = true

    //Set output dir
    val songsDownloadBaseDir = cmdLineParser.getParsedOptionValue("outdir") as File

    //Validate inputs
    if (!cmdLineParser.hasOption("url") && !cmdLineParser.hasOption("search") && !cmdLineParser.hasOption("sync")) {
        println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: --url=<intamil movie url> or --search=<moviename> or --sync options is must.").reset())
        exitProcess(7)
    }

    if (cmdLineParser.hasOption("url") && cmdLineParser.hasOption("search")) {
        println(Ansi.ansi().fg(Ansi.Color.CYAN).a("WARNING: --url and --search options are passed. --url only will be considered.").reset())
    }

    //Process the inputs
    val movieURLImmutableList = when {
        cmdLineParser.hasOption("url") -> listOf(cmdLineParser.getParsedOptionValue("url") as URL)
        cmdLineParser.hasOption("search") -> try {
            searchAndFindMovieURL(cmdLineParser.getParsedOptionValue("search") as String)
        } catch (ex: Exception) { //Master Fault handler 1
            println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: Exception while searching for movie").reset())
            println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: " + ex.message).reset())
            System.out.flush()
            System.err.flush()
            ex.printStackTrace()
            System.out.flush()
            System.err.flush()
            println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: Can not continue. Quit.").reset())
            exitProcess(20)
        }
        cmdLineParser.hasOption("sync") -> downloadLatestMovieURLs(songsDownloadBaseDir)
        else -> throw IllegalStateException("Can not determine movie URL to download from command line options. --url or --search or --sync option is must.")
    }


    //Finally Movie URLs from intamil.in is ready, Now find all songs from the movie and download them in parallel
    for (movieURL in movieURLImmutableList) {
        try {
            downloadAllSongsInMovie(movieURL, songsDownloadBaseDir)
        } catch (ex: Exception) { //Master Fault handler 2
            println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: Exception while downloading songs from $movieURL in $songsDownloadBaseDir").reset())
            println(Ansi.ansi().fg(Ansi.Color.RED).a("ERROR:: " + ex.message).reset())
            System.out.flush()
            System.err.flush()
            ex.printStackTrace()
            System.out.flush()
            System.err.flush()

        }
    }

    println(Ansi.ansi().fg(Ansi.Color.GREEN).a("INFO: Took " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime) + " Seconds to complete").reset())
    println(Ansi.ansi().fg(Ansi.Color.CYAN).a("INFO: Done").reset())

    AnsiConsole.systemUninstall()
    exitProcess(0) //Quit normally, calling explicit exitProcess allows to call shutdown hooks
}

