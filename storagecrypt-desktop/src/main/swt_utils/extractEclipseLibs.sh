#!/bin/sh

#  Copyright Pierre Sagne (12 december 2014)
#
# petrus.dev.fr@gmail.com
#
# This software is a computer program whose purpose is to encrypt and
# synchronize files on the cloud.
#
# This software is governed by the CeCILL license under French law and
# abiding by the rules of distribution of free software.  You can  use,
# modify and/ or redistribute the software under the terms of the CeCILL
# license as circulated by CEA, CNRS and INRIA at the following URL
# "http://www.cecill.info".
#
# As a counterpart to the access to the source code and  rights to copy,
# modify and redistribute granted by the license, users are provided only
# with a limited warranty  and the software's author,  the holder of the
# economic rights,  and the successive licensors  have only  limited
# liability.
#
# In this respect, the user's attention is drawn to the risks associated
# with loading,  using,  modifying and/or developing or reproducing the
# software by the user in light of its specific status of free software,
# that may mean  that it is complicated to manipulate,  and  that  also
# therefore means  that it is reserved for developers  and  experienced
# professionals having in-depth computer knowledge. Users are therefore
# encouraged to load and test the software's suitability as regards their
# requirements in conditions enabling the security of their systems and/or
# data to be ensured and,  more generally, to use and operate it in the
# same conditions as regards security.
#
# The fact that you are presently reading this means that you have had
# knowledge of the CeCILL license and that you accept its terms.

################
# This script extract SWT and jFace jars from an eclipse package

ECLIPSE_VERSION=4.5.2
SWT_ZIP_LINUX32=swt-${ECLIPSE_VERSION}-gtk-linux-x86.zip
SWT_ZIP_LINUX64=swt-${ECLIPSE_VERSION}-gtk-linux-x86_64.zip
SWT_ZIP_WIN32=swt-${ECLIPSE_VERSION}-win32-win32-x86.zip
SWT_ZIP_WIN64=swt-${ECLIPSE_VERSION}-win32-win32-x86_64.zip
SWT_ZIP_OSX64=swt-${ECLIPSE_VERSION}-cocoa-macosx-x86_64.zip
ECLIPSE_TGZ=eclipse-platform-${ECLIPSE_VERSION}-*.tar.gz

SWT_TARGET_DIR=libs/swt-lib-${ECLIPSE_VERSION} 
JFACE_TARGET_DIR=libs/jface-lib-${ECLIPSE_VERSION}

mkdir -p ${SWT_TARGET_DIR}
mkdir -p ${JFACE_TARGET_DIR}

mkdir tmp

unzip ${SWT_ZIP_LINUX32} swt.jar -d tmp
mv tmp/swt.jar ${SWT_TARGET_DIR}/swt-linux32-${ECLIPSE_VERSION}.jar

unzip ${SWT_ZIP_LINUX64} swt.jar -d tmp
mv tmp/swt.jar ${SWT_TARGET_DIR}/swt-linux64-${ECLIPSE_VERSION}.jar

unzip ${SWT_ZIP_WIN32} swt.jar -d tmp
mv tmp/swt.jar ${SWT_TARGET_DIR}/swt-win32-${ECLIPSE_VERSION}.jar

unzip ${SWT_ZIP_WIN64} swt.jar -d tmp
mv tmp/swt.jar ${SWT_TARGET_DIR}/swt-win64-${ECLIPSE_VERSION}.jar

unzip ${SWT_ZIP_OSX64} swt.jar -d tmp
mv tmp/swt.jar ${SWT_TARGET_DIR}/swt-osx64-${ECLIPSE_VERSION}.jar

cd tmp
tar xvfz ../eclipse-platform-${ECLIPSE_VERSION}-*.tar.gz
unzip ../eclipse-platform-${ECLIPSE_VERSION}-*.zip
cd ..

cp tmp/eclipse/plugins/org.eclipse.core.commands*.jar tmp/eclipse/plugins/org.eclipse.equinox.common*.jar tmp/eclipse/plugins/org.eclipse.jface*.jar ${JFACE_TARGET_DIR}

rm -rf tmp

