#!/usr/bin/env bash
sbt publishSigned || exit 1
export COURSIER_REPOSITORIES='https://maven.aliyun.com/nexus/content/groups/public|sonatype:snapshots'
CACHE_PATH=~/Library/Caches/Coursier/v1/https/
TRICKS=com/zhranklin/scala-tricks_3/
VERSION=0.2.2-SNAPSHOT
ALIYUN=maven.aliyun.com/nexus/content/groups/public/
OSS_SNAP=oss.sonatype.org/content/repositories/snapshots/
rm -rf $CACHE_PATH$ALIYUN$TRICKS$VERSION $CACHE_PATH$OSS_SNAP$TRICKS$VERSION
cs fetch com.zhranklin:scala-tricks_3:$VERSION
rm -rf $CACHE_PATH$ALIYUN$TRICKS$VERSION
cp -r $CACHE_PATH$OSS_SNAP$TRICKS$VERSION $CACHE_PATH$ALIYUN$TRICKS$VERSION
