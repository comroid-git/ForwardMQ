#!/bin/bash

#export DEBUG_ENV="yes"
export pidFile="unit.pid"
echo $$ > $pidFile
trap 'rm -f $pidFile' EXIT

if [ -z $1 ]; then
  branch="main"
else
  branch="$1"
fi

function fetch() {
  if [ -f "force_commit.txt" ]; then
    echo "Checking out commit: $(cat "force_commit.txt")"
    git checkout "$(cat "force_commit.txt")"
    return
  fi
  prevBranch="$branch"
  if [ "$(git show-ref --verify --quiet "refs/heads/$branch")" ]; then
    branch="main"
    echo "Checking out default branch: $branch"
    git checkout "$branch"
  fi
  echo "Pulling changes from $branch..."
  git pull

  branch="$prevBranch"
}

(
  cd '../japi' || exit;
  fetch
)

fetch

exec="gradle"
if [ -z "$(which "$exec")" ]; then
  exec="gradlew"
fi

$exec --no-daemon "run";
