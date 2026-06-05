# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the Limba project, a Java-based language intelligence model assistant that interfaces with LLMs (Large Language Models) such as Ollama, OpenAI, Anthropic, and Google Gemini. It provides capabilities for code analysis, generation, documentation, testing, and debugging through a command-based interface.

## Architecture

The system is built around the `LimbaMain` class which serves as the main entry point. It supports multiple LLM backends and uses LangChain4j for LLM integration. The architecture follows a command pattern where different commands (QUERY, FIND, TESTS, etc.) are processed through `LimbaCommandBase` which handles command creation and execution.

Key components:
- `LimbaMain.java`: Main application class that handles command line arguments, LLM connections, and command processing
- `LimbaCommandBase.java`: Implements command factory and various command types (QUERY, FIND, TESTS, etc.)
- `LimbaRag.java`: Handles retrieval-augmented generation for context-aware responses
- `LimbaTools.java`: Provides tools for code analysis and interaction with the codebase
- `prompts.xml`: Contains command-specific prompts for different LLM interactions

## Build and Development

### Build Process
The project uses Apache Ant for building. The main build file is `build.xml` which defines targets for:
- `setup`: Sets up the build environment
- `compile`: Compiles Java source files
- `jar`: Creates the main `limba.jar` executable
- `relayjar`: Creates `limbarelay.jar` 
- `bubbles`: Builds for integration with Bubbles IDE
- `testfred4` and `testxml`: Run tests with specific configurations

### Commands to Build
```bash
# Build the main jar
ant jar

# Build for Bubbles integration
ant bubbles

# Run tests
ant testfred4
ant testxml
```

### Running Tests
The project includes test configurations in `test.xml` and `test.in`. Tests can be run using the Ant targets:
- `ant testfred4`: Runs tests against a specific LLM server
- `ant testxml`: Runs tests using XML input files

### Development Setup
1. Ensure Java 10+ is installed
2. Set up Ollama server for local LLM access
3. Configure API keys for OpenAI, Anthropic, or Gemini in the properties file
4. Run `ant setup` to initialize build environment
5. Use `ant compile` to compile source code

## Key Features

1. **Command-based Interface**: Supports various commands like QUERY, FIND, TESTS, JAVADOC, EXPLAIN
2. **Multiple LLM Support**: Can work with Ollama, OpenAI, Anthropic, and Google Gemini
3. **Code Analysis and Generation**: Capable of generating, cleaning, and analyzing Java code
4. **RAG (Retrieval-Augmented Generation)**: Context-aware responses using codebase information
5. **Tool Integration**: Integration with code analysis tools and debugging capabilities

## Usage Patterns

The system is designed to be used via XML command files that specify operations. Commands are processed through the `LimbaMain` class which routes them to appropriate handlers in `LimbaCommandBase`.

Example usage:
```bash
java -jar limba.jar -f input.xml -l llama4:scout -h localhost -p 11434
```

## Important Files and Directories

- `javasrc/edu/brown/cs/limba/limba/`: Main Java source code
- `resources/prompts.xml`: Command prompts for different LLM interactions
- `build.xml`: Ant build configuration
- `test.xml`, `test.in`: Test input files
- `lib/`: External libraries and dependencies
- `bin/`: Executable scripts

## Common Development Tasks

1. **Adding new commands**: Extend `LimbaCommandFactory` in `LimbaCommandBase.java`
2. **Modifying prompts**: Edit `resources/prompts.xml` 
3. **Adding LLM support**: Update `LimbaMain.java` with new model handling
4. **Extending tools**: Add new tool implementations in `LimbaTools.java`
5. **Testing**: Use `ant testxml` or `ant testfred4` to run tests