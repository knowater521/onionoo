#!/usr/bin/env bash

echo "Starting provisioning."

echo "Updating package list and upgrading existing packages."
apt-get update
apt-get -y upgrade

echo "Installing required packages."
apt-get install -y libcommons-codec-java libcommons-compress-java \
libcommons-lang-java libgoogle-gson-java junit4 libservlet3.0-java \
openjdk-6-jdk ant liblog4j1.2-java tomcat6

echo "Setting up paths and creating symbolic links."
mkdir -p /srv/onionoo.torproject.org/onionoo/
cd /srv/onionoo.torproject.org/onionoo/
ln -s /vagrant/bin
ln -s /vagrant/build.xml
ln -s /vagrant/deps
ln -s /vagrant/etc
ln -s /vagrant/geoip
ln -s /vagrant/src
ln -s /vagrant/web
ln -s /srv/o
ln -s /srv/onionoo.torproject.org/onionoo/onionoo.war \
/var/lib/tomcat6/webapps/onionoo.war
chown -R vagrant:vagrant /srv/onionoo.torproject.org/

echo "Done provisioning."

