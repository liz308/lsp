# GitHub Actions Workflows Setup

This project uses GitHub Actions for continuous integration and deployment.

## Workflows

### 1. Android CI (`android-ci.yml`)
Runs automatically on push/PR to `main` or `develop` branches.

**Jobs:**
- **Lint**: Checks code quality and style
- **Build**: Builds debug APK
- **Test**: Runs unit tests
- **Build Release**: Builds release APK (only on main branch)

### 2. Android Deploy (`android-deploy.yml`)
Deploys releases to GitHub Releases and/or Google Play Store.

**Triggers:**
- Automatically on version tags (e.g., `v1.0.0`)
- Manually via workflow dispatch

## Required Secrets

To enable the workflows, add these secrets in your GitHub repository:
**Settings → Secrets and variables → Actions → New repository secret**

### For Release Builds (Required)

1. **KEYSTORE_BASE64**
   - Your Android keystore file encoded in base64
   - Generate: `base64 -i your-keystore.jks | pbcopy` (macOS) or `base64 -w 0 your-keystore.jks` (Linux)

2. **KEYSTORE_PASSWORD**
   - Password for your keystore file

3. **KEY_ALIAS**
   - Alias of the key in your keystore

4. **KEY_PASSWORD**
   - Password for the key

### For Play Store Deployment (Optional)

5. **PLAY_STORE_SERVICE_ACCOUNT_JSON**
   - Google Play Console service account JSON
   - Create in Google Play Console → Setup → API access → Service accounts

## Setup Instructions

### 1. Generate Keystore (if you don't have one)

```bash
keytool -genkey -v -keystore release.keystore -alias lsp-android -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Encode Keystore to Base64

```bash
# macOS
base64 -i release.keystore | pbcopy

# Linux
base64 -w 0 release.keystore

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Set-Clipboard
```

### 3. Add Secrets to GitHub

1. Go to your repository on GitHub
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add each secret:
   - Name: `KEYSTORE_BASE64`
   - Value: (paste the base64 string)
   - Click **Add secret**
5. Repeat for all required secrets

### 4. Test the Workflow

**Option A: Push to trigger CI**
```bash
git add .
git commit -m "Add GitHub Actions workflows"
git push origin main
```

**Option B: Create a release tag**
```bash
git tag v1.0.0
git push origin v1.0.0
```

**Option C: Manual deployment**
1. Go to **Actions** tab in GitHub
2. Select **Android Deploy** workflow
3. Click **Run workflow**
4. Choose deployment target
5. Click **Run workflow**

## Workflow Features

### CI Workflow
- ✅ Automatic linting on every push/PR
- ✅ Builds debug APK for testing
- ✅ Runs unit tests
- ✅ Uploads artifacts (APKs, reports)
- ✅ Builds release APK on main branch
- ✅ Caches Gradle dependencies for faster builds

### Deploy Workflow
- ✅ Builds signed release APKs for all architectures
- ✅ Creates GitHub releases with release notes
- ✅ Uploads to Google Play Store (internal track)
- ✅ Includes ProGuard mapping files
- ✅ Manual or automatic deployment

## Deployment Targets

### GitHub Releases
- Automatically creates releases for version tags
- Uploads all APK variants (arm64-v8a, armeabi-v7a, x86_64)
- Includes release notes
- Public download links

### Google Play Store
- Uploads to Internal Testing track
- Requires service account setup
- Includes ProGuard mapping for crash reports
- Can promote to other tracks manually

## Architecture-Specific APKs

The build produces separate APKs for each architecture:
- **arm64-v8a** (120): Modern 64-bit ARM devices (recommended)
- **x86_64** (121): Emulators and x86 devices
- **armeabi-v7a** (122): Legacy 32-bit ARM devices

Version codes are automatically incremented per architecture.

## Troubleshooting

### Build Fails: "Keystore not found"
- Ensure `KEYSTORE_BASE64` secret is set correctly
- Verify the base64 encoding has no line breaks

### Build Fails: "NDK not found"
- The workflow automatically installs NDK 26.1.10909125
- Check if the NDK version matches your `build.gradle.kts`

### Play Store Upload Fails
- Verify `PLAY_STORE_SERVICE_ACCOUNT_JSON` is valid
- Ensure the service account has "Release Manager" role
- Check that the package name matches

### Tests Fail
- Review test reports in the Actions artifacts
- Run tests locally: `./gradlew :android-app:testDebugUnitTest`

## Customization

### Change Deployment Track
Edit `.github/workflows/android-deploy.yml`:
```yaml
track: internal  # Change to: alpha, beta, production
```

### Add Slack Notifications
Add to the end of workflows:
```yaml
- name: Notify Slack
  uses: 8398a7/action-slack@v3
  with:
    status: ${{ job.status }}
    webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### Add More Tests
Edit `.github/workflows/android-ci.yml` to add instrumented tests:
```yaml
- name: Run instrumented tests
  uses: reactivecircus/android-emulator-runner@v2
  with:
    api-level: 29
    script: ./gradlew :android-app:connectedDebugAndroidTest
```

## Best Practices

1. **Always test locally first**: Run `./gradlew build` before pushing
2. **Use semantic versioning**: Tag releases as `v1.0.0`, `v1.1.0`, etc.
3. **Review artifacts**: Download and test APKs from Actions artifacts
4. **Monitor builds**: Check Actions tab regularly for failures
5. **Keep secrets secure**: Never commit keystore or passwords to git

## Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Android CI/CD Best Practices](https://developer.android.com/studio/publish/app-signing)
- [Google Play Publishing](https://developer.android.com/distribute/console)
