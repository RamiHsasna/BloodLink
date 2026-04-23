# BloodLink Developer Onboarding Checklist

Use this checklist to ensure you have everything set up correctly before starting development.

## Initial Setup (15-30 minutes)

- [ ] Cloned the repository
- [ ] PHP 8.1+ installed (`php -v`)
- [ ] Composer installed (`composer --version`)
- [ ] PostgreSQL/Supabase access verified
- [ ] Ran `composer install`
- [ ] Copied `.env.example` to `.env`
- [ ] Updated `DATABASE_URL` in `.env`
- [ ] Ran `php bin/console doctrine:query:sql "SELECT 1"` - Success!
- [ ] Ran `php bin/console doctrine:mapping:info` - All 14 entities [OK]
- [ ] Server starts: `php -S 127.0.0.1:8000 -t public`
- [ ] Can access `http://localhost:8000` without errors

## Documentation Review (30 minutes)

- [ ] Read `README.md` - Understand project overview
- [ ] Read `docs/symfony-setup.md` - Familiar with setup process
- [ ] Skimmed `docs/development-guide.md` - Know where to find patterns
- [ ] Bookmarked `docs/api-documentation.md` - Reference for API formats
- [ ] Read the log-management docs - Know that `Audit Logs` and `Alerts` are separate workspaces
- [ ] Understand the 3-layer architecture (Controller → Service → Repository)

## Development Environment (10 minutes)

- [ ] IDE/Editor set up (VS Code, PHPStorm, etc.)
- [ ] PHP Extensions: installed any needed extensions
- [ ] Database tool available (DBeaver, pgAdmin, CLI, etc.)
- [ ] Git configured (`git config user.name`, `git config user.email`)
- [ ] Can create a test file and run PHP syntax check (`php -l`)

## Code Organization Understanding (20 minutes)

- [ ] Know where Controllers are: `src/Controller/Api/`
- [ ] Know where Services are: `src/Service/`
- [ ] Know where Entities are: `src/Entity/`
- [ ] Know where Repositories are: `src/Repository/`
- [ ] Know where Utilities are: `src/Util/`
- [ ] Understand dependency injection pattern

## First Feature Implementation Checklist

When you're ready to implement your first feature:

### 1. Understand the Feature
- [ ] Read the feature requirements
- [ ] Identify which entity/entities it involves
- [ ] Check if entity already exists

### 2. Design the Service
- [ ] Identify required service methods
- [ ] Note which repositories you'll need
- [ ] List other services you'll depend on
- [ ] Sketch out the business logic steps

### 3. Implement Service
- [ ] Add constructor parameters
- [ ] Implement each method following the TODO outline
- [ ] Add error handling for edge cases
- [ ] Consider database transactions if needed
- [ ] Test service logic (unit test or manual)

### 4. Implement Controller
- [ ] Identify required endpoints
- [ ] Map to HTTP methods (GET, POST, PUT, DELETE, PATCH)
- [ ] Extract and validate request data
- [ ] Call appropriate service method
- [ ] Format response using ResponseUtil
- [ ] Return correct HTTP status codes

### 5. Test Manually
- [ ] Start server: `php -S 127.0.0.1:8000 -t public`
- [ ] Use curl, Postman, or Insomnia to test
- [ ] Verify request formats match `docs/api-documentation.md`
- [ ] Check response format (success/error structure)
- [ ] Test error cases (missing data, invalid IDs, etc.)

### 6. Document
- [ ] Update `docs/api-documentation.md` if endpoint details changed
- [ ] Add code comments for complex logic
- [ ] Update README.md if there are special setup needs

## Common Commands Reference

### Start development server
php -S 127.0.0.1:8000 -t public

### Check database connection
php bin/console doctrine:query:sql "SELECT 1"

### Verify entity mappings
php bin/console doctrine:mapping:info

### Validate schema (skip sync)
php bin/console doctrine:schema:validate --skip-sync

### Check syntax of a file
php -l src/Service/MyService.php

### Find all TODOs in code
grep -r "TODO:" src/

### Run a helper script, if one is ever needed
py path\to\script.py

### Clear cache
php bin/console cache:clear

## When You Get Stuck

**Problem**: "Where should this code go?"
**Solution**: Refer to `docs/development-guide.md` → Code Organization section

**Problem**: "What should this method look like?"
**Solution**: Find similar method in same service and use as template

**Problem**: "How do I format the response?"
**Solution**: Check `docs/api-documentation.md` for endpoint example

**Problem**: "How do I access the database?"
**Solution**: Use repository via constructor injection, follow existing patterns

**Problem**: "Doctrine mapping error"
**Solution**: Run `php bin/console doctrine:mapping:info` to see which entity has issues

**Problem**: "Service/Controller won't be instantiated"
**Solution**: Make sure constructor parameters are type-hinted with real classes

## Code Quality Checklist

Before submitting your code:

- [ ] No PHP syntax errors (`php -l src/...`)
- [ ] Follows PSR-12 style guide
- [ ] All public methods have docblocks
- [ ] Type hints on method parameters
- [ ] Error handling for edge cases
- [ ] Dependency injection for all dependencies
- [ ] No hardcoded values (use parameters)
- [ ] Appropriate HTTP status codes returned
- [ ] Responses use ResponseUtil for consistency
- [ ] Follows existing code patterns

## Git Workflow

- [ ] Create feature branch from main
- [ ] Commit regularly with clear messages
- [ ] Push to remote
- [ ] Create pull request
- [ ] Get code review
- [ ] Merge to main

## Final Checklist Before Starting Work

- [ ] All setup complete and verified
- [ ] Can run `php -S` and access API
- [ ] Can execute `doctrine:mapping:info` successfully
- [ ] Understand the project structure
- [ ] Read relevant documentation
- [ ] Know how to find TODOs
- [ ] Have a text editor/IDE ready
- [ ] Have database client ready
- [ ] Have API testing tool ready (Postman/Insomnia)

## Success Indicator

You're ready to start when:
✅ You can explain the 3-layer architecture
✅ You can find where controllers are
✅ You can find where services are
✅ You understand how dependency injection works
✅ You can start the development server
✅ You can connect to the database

---

**Next Step**: Pick your first feature from the TODO list and start implementing!

Good luck! 🚀
