#!/bin/bash

if [ -f ".env" ]; then
  source .env
fi

channelId=${channelId:-$1}
token=${token:-$2}

if [ -z "$token" ] || [ -z "$channelId" ]; then
  echo "Usage: $0 <channelId> <token>"
  echo "channelId and token can be set as environment variables, passed as arguments or read from a .env file."
  exit 1
fi

mkdir -p ./data

docker run --rm -v ./data:/out tyrrrz/discordchatexporter:stable export -t "$token" -c "$channelId" -f Json