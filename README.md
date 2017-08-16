# Download Tamil Songs from intamil website

## DownloadInTamil-EXE.jar 

DownloadInTamil-EXE.jar is a tool to download tamil MP3 songs of the given movie from [intamil website URL](http://intamil.in/). This tool downloads all the songs of the movie in parallel. It will also create correct directory / folder to keep the downloaded songs. In the directory, this tool will create movie info text file with movie details.

This tool honors HTTP proxy environment variables (http_proxy, https_proxy)

## Usage
java -jar DownloadInTamil-EXE.jar

    java -jar DownloadInTamil-EXE.jar --help

    usage: java -jar DownloadInTamil-EXE.jar [Options]
    [Options]
    -h,--help                        Shows Help message
    --no-proxy                    Dont use System Proxy Environment Variables (http_proxy, https_proxy)
    -O,--outdir <Output_Directory>   Output directory where movie specific folders will get created to download songs. Ex:C:/Temp
    -U,--url <InTamil_URL>           intamil.in movie URL to download songs. Ex: http://intamil.in/songs/Katha-Nayagan

    (c) Bakkiaraj Murugesan
    https://bakkiaraj.github.io/DownloadInTamil
    License: MIT License
    
**Note: Java 1.8 is required.**

## Example 
    java -jar DownloadInTamil-EXE.jar --url=http://intamil.in/songs/Vikram-Vedha --outdir=C:/temp
    
    INFO: Start DownloadInTamil-EXE.jar v0.0.5
    INFO: Downloading data from http://intamil.in/songs/Vikram-Vedha . Wait...
    INFO: Movie: Vikram Vedha
    INFO: Director: Pushkar Gayathri
    INFO: Music Director: Sam C S
    INFO: Cast: Madhavan, Vijay Sethupathi, Kathir, John Vijay
    INFO: ------ Songs --------
            1. Tasakku Tasakku.mp3
            2. Yaanji Yaanji.mp3
            3. Ghetto Chase.mp3
            4. Idhu Emosion.mp3
            5. Karuppu Vellai.mp3
            6. Oru Katha Sollatta.mp3
            7. Pogatha Yennavittu.mp3
            8. Sangu Sattham.mp3
            9. Yethu Dharmam.mp3
            10. Yethu Nyayam.mp3
    INFO: ------ Songs --------
    INFO: Create download dir C:\temp/Vikram Vedha
    INFO: Starting 3 download process in parallel
    INFO: Start search Ghetto_Chase.mp3 info from http://intamil.in/song/2419/Ghetto-Chase ...
    INFO: Start search Tasakku_Tasakku.mp3 info from http://intamil.in/song/2397/Tasakku-Tasakku ...
    INFO: Start search Yaanji_Yaanji.mp3 info from http://intamil.in/song/2406/Yaanji-Yaanji ...
    INFO: Downloading Tasakku_Tasakku.mp3 from URL http://intamil.in/download/2397 ...
    INFO: Downloading Yaanji_Yaanji.mp3 from URL http://intamil.in/download/2406 ...
    INFO: Downloading Ghetto_Chase.mp3 from URL http://intamil.in/download/2419 ...
    ...
    
## Download
* Latest [DownloadInTamil-EXE.jar](https://github.com/bakkiaraj/DownloadInTamil/blob/master/DownloadInTamil/exe/DownloadInTamil-EXE.jar)

* Java For [Windows](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html)

## Contribute 
This project is developed in [Kotlin](kotlinlang.org/) its a good start for developer who has limited knowledge in Java and like to learn Kotlin. 