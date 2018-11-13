#!/bin/bash

# download the page source
link="https://data.vision.ee.ethz.ch/cvl/DIV2K/"
wget -O link_source.html "$link"

# parse the links
links=$(grep -o 'http.*HR\.zip' link_source.html)
rm -f link_source.html

# download the zips
for link in $links; do
     wget -c $link 
done

# unzip the zips
zipfiles=$(echo $links | grep -o '[a-zA-Z_2-4]*.zip')
for zipfile in $zipfiles; do
    unzip $zipfile
done 

# move the directories
mkdir -p div2k
find -mindepth 1 -maxdepth 1 -type d -name 'DIV2K*' -exec mv {} div2k \;

# remove the zips
rm -f $zipfiles
