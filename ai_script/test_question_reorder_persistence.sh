#!/bin/bash
cd "$(dirname "$0")/.."
echo "Running QuestionReorderTest..."
mvn test -Dtest=QuestionReorderTest
