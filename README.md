Offene Bibel Parser
===================

Building
--------
To build the project:

- install a Java 7 (or larger)
    - automatically

            # ubuntu
            apt-get install openjdk-7-jdk
            # redhat
            yum install java-1.7.0-openjdk

    - manually
        - download and unzip/install from <http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html>
        - `export JAVA_HOME=/wherever/you/unzipped/jdk1.7.0_17/`
- install maven
    - automatically

            # ubuntu
            sudo apt-get install maven2
            # redhat
            yum install maven2

    - manually from <http://maven.apache.org/download.html>
- run `build.sh`

The results reside in the *install/* folder.
To run the converter `install/bin/exporter.sh --help`.

The converter will download the translation from the Offene Bibel and create two *.osis* files in the *install/results/* folder.
The exporter caches all files it downloads to *install/tmp/pageCache/*. They won't be redownloaded. To redownload a file, just delete it in the cache.

There is also a convenience script that creates two sword modules, copies them to *~/.sword* and creates a .zip archive: `install/bin/swordConverter.sh`.

And then there is the validator that checks a given Wiki page for validity.
You can run it as follows: `install/bin/validator.sh --help`.

Web viewer file generation
--------------------------
The parser can generate files suitable as input for the *Offene Bibel Web Viewer*. It generates a file structure as follows:

    webResults/Matthäus_12_lf
    webResults/Matthäus_12_sf
    webResults/Matthäus_12_ls
    webResults/generated.index

Multiple runs will overwrite both, chapter files and the status file. The *generated.index* file will have a comment at the start indicating the date and parameters used for generation.

