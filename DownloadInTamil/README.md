# Download Tamil Songs from intamil website

## [DownloadInTamil-EXE.jar](https://github.com/bakkiaraj/DownloadInTamil/blob/master/DownloadInTamil/exe/DownloadInTamil-EXE.jar)

DownloadInTamil-EXE.jar is a tool to download tamil MP3 songs of the given movie from [intamil website URL](http://intamil.in/) or it can search the movie name and download the songs.
This tool downloads all the songs of the movie in parallel.
It will also create correct directory / folder to keep the downloaded songs. In the directory, this tool will create movie info text file with movie details.

This tool honors HTTP proxy environment variables (http_proxy, https_proxy).

DownloadInTamil-EXE.jar works in Linux, Mac and Windows OS.

**Note: [Java (JRE)](http://www.oracle.com/technetwork/java/javase/downloads/index.html) 1.8.x or higher version is required.**

## Usage
java -jar DownloadInTamil-EXE.jar <Options>

    usage: java -jar DownloadInTamil-EXE.jar [Options]

     DownloadInTamil-EXE.jar v0.2.0 is a tool to download all the movie songs from http://intamil.in website.
     intamil.in website do not have option to download all the songs of a movie at one go. This tool download all songs in parallel. This tool also can search movie from intamil
    website.

    [Options]
     -h,--help                          Shows Help message
        --no-proxy                      Dont honor System Proxy Environment Variables (http_proxy, https_proxy)
        --outdir <Output_Directory>     Output directory where movie specific folders will get created to download songs. Ex:C:/Temp
        --search <intamil_Movie_Name>   Tamil Movie Name to search in intamil.in and download songs. Ex: vivegam . [Optional when --url option is provided.]
        --url <intamil_movie_URL>       intamil.in movie URL to download songs. Ex: http://intamil.in/songs/Mersal . [Optional when --search option is provided.]

     (c) Bakkiaraj Murugesan
     https://bakkiaraj.github.io/DownloadInTamil
     License: MIT License

## Example 1 - Download the movie song using movie name search in intamil website
    java -jar DownloadInTamil-EXE.jar --outdir=C:/temp --search=vivegam

    INFO: Start DownloadInTamil-EXE.jar v0.2.0
    INFO: Using Java  1.8
    INFO: Searching Movie Name contains [vivegam] in intamil website
    INFO: Downloading Movie Index from http://intamil.in/movie-alphabet/v Wait...
    INFO: Done Downloaded Movie Index from http://intamil.in/movie-alphabet/v
    INFO: Found Movie Name: Vivegam , in URL: http://intamil.in/songs/Vivegam
    INFO: Downloading data from http://intamil.in/songs/Vivegam . Wait...
    INFO: Movie: Vivegam
    INFO: Director: Siva
    INFO: Music Director: Anirudh Ravichander
    INFO: Cast: Ajith, Vivek Oberoi, Kajal Aggarwal, Akshara Haasan
    INFO: ------ Songs --------
            1. Surviva.mp3
            2. KadhalaadaKadhal Aada.mp3
            3. Thalai Viduthalai.mp3
    INFO: ------ Songs --------
    INFO: Create download dir C:\temp/Vivegam
    INFO: Starting 3 download process in parallel
    INFO: Start search Surviva.mp3 info from http://intamil.in/song/2427/Surviva ...
    INFO: Downloading Surviva.mp3 from URL http://intamil.in/download/2427 ...
    INFO: Start search KadhalaadaKadhal_Aada.mp3 info from http://intamil.in/song/2442/KadhalaadaKadhal-Aada ...
    INFO: Downloading KadhalaadaKadhal_Aada.mp3 from URL http://intamil.in/download/2442 ...
    INFO: Start search Thalai_Viduthalai.mp3 info from http://intamil.in/song/2443/Thalai-Viduthalai ...
    INFO: Downloading Thalai_Viduthalai.mp3 from URL http://intamil.in/download/2443 ...
    INFO: Done Downloading Thalai_Viduthalai.mp3 from http://intamil.in/song/2443/Thalai-Viduthalai into C:\temp/Vivegam
    INFO: Done Downloading Surviva.mp3 from http://intamil.in/song/2427/Surviva into C:\temp/Vivegam
    INFO: Done Downloading KadhalaadaKadhal_Aada.mp3 from http://intamil.in/song/2442/KadhalaadaKadhal-Aada into C:\temp/Vivegam
    INFO: 3 files downloaded in C:\temp/Vivegam
    INFO: Took 57 Seconds to complete
    INFO: Done

    dir c:\temp\Vivegam
    18/08/2017  05:33 PM    <DIR>          .
    18/08/2017  05:33 PM    <DIR>          ..
    18/08/2017  05:33 PM               128 info.txt
    18/08/2017  05:33 PM         8,854,639 KadhalaadaKadhal_Aada.mp3
    18/08/2017  05:33 PM         7,508,003 Surviva.mp3
    18/08/2017  05:33 PM         6,837,137 Thalai_Viduthalai.mp3

    type c:\Temp\Vivegam\info.txt
    Movie: Vivegam
    Director: Siva
    Music Director: Anirudh Ravichander
    Cast: Ajith, Vivek Oberoi, Kajal Aggarwal, Akshara Haasan

## Example 2 - Download the movie song using intamil website fully qualified movie URL
    java -jar DownloadInTamil-EXE.jar --outdir=C:/temp --url=http://intamil.in/songs/Katha-Nayagan

    INFO: Start DownloadInTamil-EXE.jar v0.2.0
    INFO: Using Java  1.8
    INFO: Downloading data from http://intamil.in/songs/Katha-Nayagan . Wait...
    INFO: Movie: Katha Nayagan
    INFO: Director: Muruganandham
    INFO: Music Director: Sean Roldan
    INFO: Cast: Vishnu, Catherine Tresa
    INFO: ------ Songs --------
            1. On Nenappu.mp3
    INFO: ------ Songs --------
    INFO: Create download dir C:\temp/Katha Nayagan
    INFO: Starting 3 download process in parallel
    INFO: Start search On_Nenappu.mp3 info from http://intamil.in/song/2444/On-Nenappu ...
    INFO: Downloading On_Nenappu.mp3 from URL http://intamil.in/download/2444 ...
    INFO: Done Downloading On_Nenappu.mp3 from http://intamil.in/song/2444/On-Nenappu into C:\temp/Katha Nayagan
    INFO: 1 files downloaded in C:\temp/Katha Nayagan
    INFO: Took 21 Seconds to complete
    INFO: Done

## Example 3 - Try Download the movie which is not yet uploaded in intamil website - Error message example
    java -jar DownloadInTamil-EXE.jar --outdir=C:/temp --search=future

    INFO: Start DownloadInTamil-EXE.jar v0.2.0
    INFO: Using Java  1.8
    INFO: Searching Movie Name contains [future] in intamil website
    INFO: Downloading Movie Index from http://intamil.in/movie-alphabet/f Wait...
    INFO: Done Downloaded Movie Index from http://intamil.in/movie-alphabet/f
    ERROR:: Can not find future movie in http://intamil.in/movie-alphabet/f using regex (?i)future
    ERROR:: May be movie songs are not yet uploaded. Can not continue. Quit.


## Download
* Latest [DownloadInTamil-EXE.jar](https://github.com/bakkiaraj/DownloadInTamil/blob/master/DownloadInTamil/exe/DownloadInTamil-EXE.jar)

* Java For [Windows](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html)

## Contribute
This project is developed in [Kotlin](kotlinlang.org/) its a good start for developer who has limited knowledge in Java and like to learn Kotlin.