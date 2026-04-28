# GitHub Release Skill

You are responsible for creating GitHub releases with debug APK assets for the **Expense Analyst** Android app.

## When to use

After any code change that results in a new debug APK build, create a GitHub release so the latest APK is always available for download.

## Steps

### 1. Build the APK
```bash
cd "/Users/anup/AI Workspace/expense-analyst"
./gradlew clean assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

### 2. Commit and push
```bash
git add -A
git commit -m "<conventional commit message>"
git push origin main
```

### 3. Create the release
```bash
gh release create v<VERSION>-debug \
  --title "v<VERSION> - <short description>" \
  --notes "<release notes>" \
  app/build/outputs/apk/debug/app-debug.apk
```

## Versioning

- Use semver: `v<major>.<minor>.<patch>-debug`
- Bump patch for bug fixes, minor for features, major for breaking changes
- Check the latest tag first: `gh release list --limit 1`

## Auth

- The repo uses `gh auth setup-git` for HTTPS push auth via the GitHub CLI
- If push fails with "repository not found", re-run `gh auth setup-git`

## Repo

- Remote: `https://github.com/psanup89-rgb/expense-analyst.git`
- Visibility: public
