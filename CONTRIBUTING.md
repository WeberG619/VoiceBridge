# Contributing to VoiceBridge 🤝

Thank you for your interest in contributing to VoiceBridge! We welcome contributions from developers, translators, accessibility experts, and skill template creators.

## 🚀 Quick Start

1. **Fork** the repository
2. **Clone** your fork: `git clone https://github.com/YOUR_USERNAME/VoiceBridge.git`
3. **Create** a feature branch: `git checkout -b feature/amazing-feature`
4. **Make** your changes
5. **Test** your changes thoroughly
6. **Submit** a pull request

## 🎯 Types of Contributions

### 🔧 Code Contributions
- Bug fixes and improvements
- New features and enhancements
- Performance optimizations
- Security improvements

### 🌍 Translation & Localization
- Add new language support for UI strings
- Create localized skill templates for your region
- Improve existing translations for accuracy
- Test voice recognition in different languages

### ♿ Accessibility Improvements
- Test with screen readers and accessibility tools
- Suggest WCAG compliance improvements
- Document accessibility best practices
- Improve navigation and interaction patterns

### 📋 Skill Template Creation
- Create skill templates for new form types
- Add region-specific government forms
- Contribute healthcare, employment, or educational templates
- Improve existing skill templates

## 📝 Development Guidelines

### Code Style
- Follow existing Kotlin/Android conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Keep functions focused and small

### Testing
- Add unit tests for new functionality
- Test on multiple Android API levels
- Validate accessibility features
- Run the complete test suite before submitting

### Documentation
- Update relevant documentation
- Add inline code comments
- Update README.md if needed
- Document new features and APIs

## 🛠️ Skill Template Guidelines

### Creating New Skills
1. **Plan your skill**:
   - Identify the target form or workflow
   - Research required fields and validation
   - Consider accessibility requirements

2. **Create the YAML file**:
   ```yaml
   id: unique_skill_id
   language: en
   name: "Descriptive Skill Name"
   description: "Clear description of what this skill does"
   version: "1.0"
   category: "government|healthcare|employment|financial|education"
   ```

3. **Validate your skill**:
   ```bash
   ./tools/validate-skills.sh path/to/your-skill.yaml
   ```

4. **Test thoroughly**:
   - Test with voice input
   - Verify field mapping
   - Check validation rules

### Skill Template Best Practices
- Use clear, conversational prompts
- Provide helpful hints for complex fields
- Include proper validation patterns
- Support multiple languages when possible
- Follow accessibility guidelines

## 🌍 Translation Guidelines

### Adding a New Language
1. **Create string resources**:
   - Copy `android/app/src/main/res/values/strings.xml`
   - Create `values-{language_code}/strings.xml`
   - Translate all strings accurately

2. **Add language support**:
   - Update `LanguageManager.kt` supported languages list
   - Add voice command triggers for the new language
   - Test voice recognition accuracy

3. **Create localized skills**:
   - Translate existing skill templates
   - Adapt for local regulations and forms
   - Validate cultural appropriateness

### Translation Quality
- Use natural, conversational language
- Consider cultural context and local terminology
- Test with native speakers
- Maintain consistency across the app

## ♿ Accessibility Guidelines

### Testing Requirements
- **Screen Readers**: Test with TalkBack and other Android screen readers
- **Navigation**: Verify keyboard and touch navigation works properly
- **Contrast**: Ensure adequate color contrast ratios
- **Text Size**: Test with large text settings enabled

### Accessibility Checklist
- [ ] All interactive elements are accessible
- [ ] Content descriptions are meaningful
- [ ] Focus order is logical
- [ ] No information conveyed by color alone
- [ ] Touch targets are at least 48dp
- [ ] Text is readable at 200% zoom

## 🔒 Security & Privacy

### Security Guidelines
- Never expose sensitive user data
- Follow Android security best practices
- Use encrypted storage for sensitive information
- Validate all user inputs

### Privacy Requirements
- Maintain offline-first architecture
- Document any data collection clearly
- Respect user privacy preferences
- Follow GDPR and accessibility standards

## 📋 Pull Request Process

### Before Submitting
1. **Test thoroughly** on multiple devices/API levels
2. **Run validation tools**: `./tools/validate-skills.sh` for skills
3. **Update documentation** if needed
4. **Add tests** for new functionality
5. **Check accessibility** compliance

### PR Description Template
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update
- [ ] Translation/localization
- [ ] Accessibility improvement
- [ ] Skill template

## Testing
- [ ] Tested on Android API 24+
- [ ] Tested accessibility features
- [ ] Validated skill templates
- [ ] Added/updated tests

## Screenshots (if applicable)
Add screenshots to help explain your changes
```

## 🐛 Bug Reports

### Issue Template
```markdown
**Bug Description**
Clear description of the bug

**Steps to Reproduce**
1. Step 1
2. Step 2
3. See error

**Expected Behavior**
What should happen

**Environment**
- Android version:
- Device model:
- App version:
- Language settings:

**Additional Context**
Any other relevant information
```

## 💡 Feature Requests

### Proposal Template
```markdown
**Feature Description**
Clear description of the proposed feature

**Problem Statement**
What problem does this solve?

**Proposed Solution**
How should this work?

**Alternatives Considered**
Other approaches you've considered

**Additional Context**
Mock-ups, examples, or references
```

## 🏷️ Good First Issues

Looking for ways to contribute? Check out issues labeled:
- `good-first-issue` - Perfect for newcomers
- `help-wanted` - Community contributions welcome
- `translation` - Translation work needed
- `accessibility` - Accessibility improvements
- `skill-template` - New skill templates needed

## 📞 Getting Help

- **General Questions**: [GitHub Discussions](https://github.com/voicebridge/voicebridge/discussions)
- **Bug Reports**: [GitHub Issues](https://github.com/voicebridge/voicebridge/issues)
- **Development Chat**: [Discord Community](https://discord.gg/voicebridge)
- **Email**: dev@voicebridge.app

## 📜 Code of Conduct

We are committed to providing a welcoming and inclusive environment for all contributors. Please read and follow our [Code of Conduct](CODE_OF_CONDUCT.md).

## 🎉 Recognition

Contributors will be recognized in:
- README.md acknowledgments
- Release notes for significant contributions
- Special contributor badges in Discord
- Annual contributor appreciation posts

---

Thank you for helping make VoiceBridge better for everyone! 🎙️✨