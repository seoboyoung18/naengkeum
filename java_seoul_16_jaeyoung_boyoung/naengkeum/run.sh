#!/bin/bash
export OPENAI_API_KEY=$(grep '^OPENAI_API_KEY=' .env | cut -d= -f2 | tr -d '\r\n')
./mvnw spring-boot:run
