#!/usr/bin/env python3
"""
VoiceBridge Skill Template Validator (skill-lint)

Validates YAML skill templates for schema compliance and potential runtime issues.
Prevents crashes from malformed skill files contributed by volunteers.

Usage:
    python skill-lint.py [skill_file.yaml] [--strict] [--output json]
    python skill-lint.py --validate-all skills/
"""

import os
import sys
import yaml
import json
import re
import argparse
from typing import Dict, List, Any, Optional, Tuple
from pathlib import Path

class SkillValidator:
    """Validates VoiceBridge skill template files"""
    
    REQUIRED_FIELDS = {
        'id': str,
        'language': str, 
        'name': str,
        'description': str,
        'version': str,
        'prompts': list
    }
    
    OPTIONAL_FIELDS = {
        'category': str,
        'postprocess': list,
        'accessibility': dict,
        'commands': list
    }
    
    PROMPT_REQUIRED = {
        'field': str,
        'ask': str,
        'type': str
    }
    
    PROMPT_OPTIONAL = {
        'hint': str,
        'required': bool,
        'validation': str,
        'format': str,
        'options': list,
        'min': int,
        'max': int,
        'default': str,
        'depends_on': str,
        'show_when': str
    }
    
    VALID_FIELD_TYPES = {
        'name', 'email', 'phone', 'address', 'date', 'ssn', 
        'text', 'number', 'currency', 'select', 'textarea'
    }
    
    SUPPORTED_LANGUAGES = {
        'en', 'es', 'pt', 'fr', 'de', 'it', 'zh', 'ja', 'ko', 'ru', 'ar', 'hi'
    }
    
    def __init__(self, strict_mode: bool = False):
        self.strict_mode = strict_mode
        self.errors = []
        self.warnings = []
        
    def validate_file(self, file_path: str) -> Tuple[bool, List[str], List[str]]:
        """Validate a single skill file"""
        self.errors = []
        self.warnings = []
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                data = yaml.safe_load(f)
                
            if not isinstance(data, dict):
                self.errors.append("Root element must be a dictionary/object")
                return False, self.errors, self.warnings
                
            self._validate_schema(data)
            self._validate_semantic(data)
            self._validate_string_lengths(data)
            self._validate_regex_patterns(data)
            
        except yaml.YAMLError as e:
            self.errors.append(f"YAML parsing error: {e}")
        except FileNotFoundError:
            self.errors.append(f"File not found: {file_path}")
        except Exception as e:
            self.errors.append(f"Unexpected error: {e}")
            
        return len(self.errors) == 0, self.errors, self.warnings
    
    def _validate_schema(self, data: Dict[str, Any]) -> None:
        """Validate basic schema structure"""
        
        # Check required fields
        for field, expected_type in self.REQUIRED_FIELDS.items():
            if field not in data:
                self.errors.append(f"Missing required field: {field}")
                continue
                
            if not isinstance(data[field], expected_type):
                self.errors.append(f"Field '{field}' must be of type {expected_type.__name__}")
                
        # Check optional fields types
        for field, expected_type in self.OPTIONAL_FIELDS.items():
            if field in data and not isinstance(data[field], expected_type):
                self.errors.append(f"Field '{field}' must be of type {expected_type.__name__}")
                
        # Validate prompts structure
        if 'prompts' in data and isinstance(data['prompts'], list):
            for i, prompt in enumerate(data['prompts']):
                self._validate_prompt(prompt, i)
                
    def _validate_prompt(self, prompt: Dict[str, Any], index: int) -> None:
        """Validate individual prompt structure"""
        
        if not isinstance(prompt, dict):
            self.errors.append(f"Prompt {index} must be a dictionary")
            return
            
        # Check required prompt fields
        for field, expected_type in self.PROMPT_REQUIRED.items():
            if field not in prompt:
                self.errors.append(f"Prompt {index} missing required field: {field}")
                continue
                
            if not isinstance(prompt[field], expected_type):
                self.errors.append(f"Prompt {index} field '{field}' must be of type {expected_type.__name__}")
                
        # Check optional prompt fields
        for field, expected_type in self.PROMPT_OPTIONAL.items():
            if field in prompt and not isinstance(prompt[field], expected_type):
                self.errors.append(f"Prompt {index} field '{field}' must be of type {expected_type.__name__}")
                
        # Validate field type
        if 'type' in prompt and prompt['type'] not in self.VALID_FIELD_TYPES:
            self.errors.append(f"Prompt {index} has invalid type '{prompt['type']}'. Valid types: {', '.join(self.VALID_FIELD_TYPES)}")
            
    def _validate_semantic(self, data: Dict[str, Any]) -> None:
        """Validate semantic correctness"""
        
        # Check language support
        if 'language' in data and data['language'] not in self.SUPPORTED_LANGUAGES:
            self.warnings.append(f"Language '{data['language']}' may not be fully supported. Supported: {', '.join(self.SUPPORTED_LANGUAGES)}")
            
        # Check ID format
        if 'id' in data:
            if not re.match(r'^[a-z][a-z0-9_]*$', data['id']):
                self.errors.append("ID must start with lowercase letter and contain only lowercase letters, numbers, and underscores")
                
        # Check version format
        if 'version' in data:
            if not re.match(r'^\d+\.\d+(\.\d+)?$', data['version']):
                self.warnings.append("Version should follow semantic versioning (e.g., '1.0.0')")
                
        # Validate field name uniqueness
        if 'prompts' in data:
            field_names = []
            for prompt in data['prompts']:
                if 'field' in prompt:
                    if prompt['field'] in field_names:
                        self.errors.append(f"Duplicate field name: {prompt['field']}")
                    field_names.append(prompt['field'])
                    
        # Check dependencies
        self._validate_dependencies(data)
        
    def _validate_dependencies(self, data: Dict[str, Any]) -> None:
        """Validate field dependencies"""
        
        if 'prompts' not in data:
            return
            
        field_names = {prompt.get('field') for prompt in data['prompts'] if 'field' in prompt}
        
        for prompt in data['prompts']:
            if 'depends_on' in prompt:
                depends_on = prompt['depends_on']
                if depends_on not in field_names:
                    self.errors.append(f"Field '{prompt.get('field')}' depends on non-existent field '{depends_on}'")
                    
    def _validate_string_lengths(self, data: Dict[str, Any]) -> None:
        """Validate string lengths to prevent UI issues"""
        
        # Check main fields
        if 'name' in data and len(data['name']) > 50:
            self.warnings.append("Name is very long (>50 chars) - may cause UI issues")
            
        if 'description' in data and len(data['description']) > 200:
            self.warnings.append("Description is very long (>200 chars) - may cause UI issues")
            
        # Check prompt strings
        if 'prompts' in data:
            for i, prompt in enumerate(data['prompts']):
                if 'ask' in prompt and len(prompt['ask']) > 100:
                    self.warnings.append(f"Prompt {i} question is very long (>100 chars) - may cause UI issues")
                    
                if 'hint' in prompt and len(prompt['hint']) > 150:
                    self.warnings.append(f"Prompt {i} hint is very long (>150 chars) - may cause UI issues")
                    
    def _validate_regex_patterns(self, data: Dict[str, Any]) -> None:
        """Validate regex patterns for syntax errors"""
        
        if 'prompts' not in data:
            return
            
        for i, prompt in enumerate(data['prompts']):
            if 'validation' in prompt:
                try:
                    re.compile(prompt['validation'])
                except re.error as e:
                    self.errors.append(f"Prompt {i} has invalid regex pattern: {e}")

def validate_directory(directory: str, strict: bool = False) -> Dict[str, Any]:
    """Validate all skill files in a directory"""
    
    results = {
        'total_files': 0,
        'valid_files': 0,
        'files_with_errors': 0,
        'files_with_warnings': 0,
        'details': []
    }
    
    validator = SkillValidator(strict)
    
    for file_path in Path(directory).rglob('*.yaml'):
        if file_path.name.startswith('.'):
            continue
            
        results['total_files'] += 1
        
        is_valid, errors, warnings = validator.validate_file(str(file_path))
        
        file_result = {
            'file': str(file_path),
            'valid': is_valid,
            'errors': errors,
            'warnings': warnings
        }
        
        results['details'].append(file_result)
        
        if is_valid:
            results['valid_files'] += 1
        else:
            results['files_with_errors'] += 1
            
        if warnings:
            results['files_with_warnings'] += 1
            
    return results

def main():
    parser = argparse.ArgumentParser(description='VoiceBridge Skill Template Validator')
    parser.add_argument('path', help='Skill file or directory to validate')
    parser.add_argument('--strict', action='store_true', help='Enable strict validation mode')
    parser.add_argument('--output', choices=['text', 'json'], default='text', help='Output format')
    parser.add_argument('--validate-all', action='store_true', help='Validate all files in directory')
    
    args = parser.parse_args()
    
    if args.validate_all or os.path.isdir(args.path):
        # Directory validation
        results = validate_directory(args.path, args.strict)
        
        if args.output == 'json':
            print(json.dumps(results, indent=2))
        else:
            print(f"📋 Validation Results for {args.path}")
            print(f"━" * 50)
            print(f"Total files: {results['total_files']}")
            print(f"✅ Valid files: {results['valid_files']}")
            print(f"❌ Files with errors: {results['files_with_errors']}")
            print(f"⚠️  Files with warnings: {results['files_with_warnings']}")
            print()
            
            for detail in results['details']:
                status = "✅" if detail['valid'] else "❌"
                print(f"{status} {detail['file']}")
                
                for error in detail['errors']:
                    print(f"   ❌ {error}")
                    
                for warning in detail['warnings']:
                    print(f"   ⚠️  {warning}")
                    
                if detail['errors'] or detail['warnings']:
                    print()
                    
        sys.exit(0 if results['files_with_errors'] == 0 else 1)
        
    else:
        # Single file validation
        validator = SkillValidator(args.strict)
        is_valid, errors, warnings = validator.validate_file(args.path)
        
        if args.output == 'json':
            result = {
                'file': args.path,
                'valid': is_valid,
                'errors': errors,
                'warnings': warnings
            }
            print(json.dumps(result, indent=2))
        else:
            print(f"📋 Validating {args.path}")
            print(f"━" * 50)
            
            if is_valid:
                print("✅ Skill template is valid!")
            else:
                print("❌ Skill template has errors:")
                for error in errors:
                    print(f"   • {error}")
                    
            if warnings:
                print("\n⚠️  Warnings:")
                for warning in warnings:
                    print(f"   • {warning}")
                    
        sys.exit(0 if is_valid else 1)

if __name__ == '__main__':
    main()