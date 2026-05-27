#!/bin/bash

# Try to get the project name from the container labels
PROJECT_NAME=$(docker inspect $(hostname) --format '{{ index .Config.Labels "com.docker.compose.project" }}' 2>/dev/null)

if [ -n "$PROJECT_NAME" ]; then
    echo "Detected Docker Compose project: $PROJECT_NAME"
    # Restart the entire project
    docker compose -p "$PROJECT_NAME" restart
else
    echo "Could not detect Docker Compose project. Restarting current container ($(hostname)) only."
    docker restart $(hostname)
fi