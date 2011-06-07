#!/bin/bash

ship="ncb-data-collection"

dir="$ship"
if [ -d $dir ]; then
	echo "$dir exists"
else
	mkdir $dir
fi

dir="$ship/server"
if [ -d $dir ]; then
	echo "$dir exists"
else
	mkdir $dir
fi

cp server/collection-server.py    $ship/server/
cp server/config.py               $ship/server/

dir="$ship/control"
if [ -d $dir ]; then
	echo "$dir exists"
else
	mkdir $dir
fi

cp control/collection-control.py  $ship/control/
cp control/config.py              $ship/control/
cp control/server-monitor.py      $ship/control/

dir="$ship/test"
if [ -d $dir ]; then
	echo "$dir exists"
else
	mkdir $dir
fi

cp test/server-test.py            $ship/test/
cp test/config.py                 $ship/test/

pushd doc
texi2pdf docs.tex
popd

dir="$ship/docs"
if [ -d $dir ]; then
	echo "$dir exists"
else
	mkdir $dir
fi

cp doc/docs.pdf                   $ship/docs/

cp -R extensions                  $ship/extensions

if [ -e $ship.tar.gz ]; then
	echo Removing tar.gz file.
	rm $ship.tar.gz
fi
tar cvzf $ship.tar.gz $ship

if [ -e $ship.zip ]; then
	echo Removing zipfile.
	rm $ship.zip
fi
zip -r $ship.zip $ship
