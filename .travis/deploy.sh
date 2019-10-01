#!/usr/bin/env bash
set -ex

setup_git() {
  echo -e "machine github.com\n  login $GITHUB_AUTH_TOKEN" >> ~/.netrc
  git config --global user.email "${GIT_USER_EMAIL}"
  git config --global user.name "${GIT_USER_NAME}"
}

if [[ ${TRAVIS_BRANCH} == master ]] ; then
  setup_git
  git checkout master
  sbt ++$TRAVIS_SCALA_VERSION 'release with-defaults skip-tests'
else
  sbt ++$TRAVIS_SCALA_VERSION publishSigned
fi
