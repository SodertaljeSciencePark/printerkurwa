# Contributing to Printerkurwa

Thank you for your interest in contributing to Printerkurwa! This document provides guidelines and instructions for contributing to the project.

## Code of Conduct

Be respectful and professional in all interactions. We're committed to providing a welcoming and inclusive environment for all contributors.

## Getting Started

### 1. Fork and Clone

```bash
git clone https://github.com/SiencePark/Prinvue.git
cd printerkurwa
```

### 2. Set Up Development Environment

```bash
# Install Java 25
java -version  # Should show Java 25+

# Build dependencies
./mvnw clean install
```

### 3. Create a Branch

```bash
git checkout -b feature/your-feature-name
# or for bug fixes:
git checkout -b fix/issue-description
```

## Development Workflow

### Code Style

- Use meaningful variable and method names

### Before Committing

1. **Run tests:**
   ```bash
   ./mvnw test
   ```

2. **Build the project:**
   ```bash
   ./mvnw clean build
   ```

3. **Format code (if configured):**
   ```bash
   ./mvnw fmt:format
   ```

### Commit Messages

Write clear, descriptive commit messages:

```
feat: add printer status endpoint

- Implement GET /api/printers/{id}/status
- Add real-time status polling
- Update models for status response

Fixes #123
```

**Commit message format:**
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `refactor:` - Code refactoring without feature changes
- `test:` - Adding or updating tests
- `chore:` - Build, dependencies, configuration

## Adding Features

### 1. Database Changes

If your feature requires database schema changes:
- Add migration SQL to `sql/` directory
- Update `sql/init.sql` for fresh deployments
- Document the changes in commit message

### 2. API Endpoints

When adding new REST endpoints:
- Add controller method in `Controllers/`
- Add service logic in `Services/`
- Add repository method if needed in `Repositories/`
- Include validation and error handling
- Add unit tests in `src/test/`

### 3. Printer Integration

For new printer types or features:
- Create strategy in `Strategies/`
- Add facade in `Facades/`
- Update Models as needed
- Ensure backward compatibility

### 4. Testing Requirements

- Minimum 70% code coverage for new code
- All public methods should have tests
- Include both happy path and error cases

Example test:
```java
@SpringBootTest
class PrinterControllerTests {

    @MockBean
    private PrinterService printerService;

    @Test
    void testGetPrinter() {
        // Arrange
        Printer printer = new Printer();
        when(printerService.getPrinter(1L)).thenReturn(printer);

        // Act & Assert
        // assertions here
    }
}
```

## Pull Request Process

### 1. Before Pushing

```bash
# Update your branch with latest main
git fetch origin
git rebase origin/main

# Run full test suite
./mvnw clean test
```

### 2. Push and Create PR

```bash
git push origin feature/your-feature-name
```

Then create a Pull Request on GitHub with:

- **Title:** Clear, concise description
- **Description:** 
  - What changes are included
  - Why these changes are needed
  - Any related issues (#123)
  - Testing performed
- **Screenshots:** If UI-related
- **Breaking changes:** Clearly marked if applicable

### 3. PR Template

```markdown
## Description
Brief summary of changes

## Related Issue
Fixes #123

## Type of Change
- [ ] New feature
- [ ] Bug fix
- [ ] Documentation
- [ ] Refactoring

## Testing Done
- [ ] Unit tests added/updated
- [ ] Integration tests passed
- [ ] Manual testing performed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Tests pass locally
- [ ] Documentation updated
- [ ] No breaking changes (or clearly documented)
```

## Code Review

- Maintainers will review your PR within 1 week
- Feedback should be addressed with new commits
- Once approved, your PR will be merged to main

## Reporting Issues

### Bug Reports

Include:
- Java version and OS
- Spring Boot version
- Steps to reproduce
- Expected behavior
- Actual behavior
- Error logs/stack traces

### Feature Requests

Describe:
- Use case and motivation
- Proposed solution
- Alternative approaches considered
- Example API usage (if applicable)

## Building and Testing

### Running Tests

```bash
# All tests
./mvnw test

# Specific test class
./mvnw test -Dtest=PrinterControllerTests

# With coverage report
./mvnw test jacoco:report
```

### Building for Production

```bash
./mvnw clean package -DskipTests
# Output: target/printerkurwa-1.0.0.jar
```

### Docker Build

```bash
docker build -t printerkurwa:dev .
docker run -p 8080:8080 printerkurwa:dev
```

## Documentation

- Update README.md for user-facing changes
- Add Javadoc for public methods
- Document API changes in relevant controller classes
- Update CHANGELOG.md with your changes

## Questions?

- Open a GitHub Discussion
- Check existing issues and PRs
- Review the official documentation in `/public/docs/`

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to Printerkurwa!
