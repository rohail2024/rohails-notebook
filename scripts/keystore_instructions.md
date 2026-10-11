
# Create and use a release keystore (on-phone)

You only do this once. Keep your passwords safe.

## Option A: AndroidIDE Terminal
1. Open AndroidIDE → Terminal tab.
2. Run:
   ```sh
   keytool -genkeypair -v -keystore release.keystore -storetype JKS      -keyalg RSA -keysize 2048 -validity 36500      -alias rohail_release
   ```
3. You'll be prompted for:
   - Keystore password (STORE_PASSWORD)
   - Key password (KEY_PASSWORD)
   - First/Last name (you can put Rohail)
   - Organizational info (you can skip/press enter)
4. Base64 encode the keystore for GitHub Actions:
   ```sh
   base64 release.keystore > release.keystore.b64
   ```
5. Open `release.keystore.b64` in a text viewer, copy ALL text.

## Option B: Termux
1. Install OpenJDK in Termux: `pkg install openjdk-17`
2. Same commands as above.

## Add GitHub Secrets
In your GitHub repo → Settings → Secrets and variables → Actions → New repository secret:
- `ANDROID_KEYSTORE_BASE64` = paste the base64 you copied
- `KEY_ALIAS` = `rohail_release` (or your alias)
- `KEY_PASSWORD` = your key password
- `STORE_PASSWORD` = your keystore password

## Build
- Push the project to GitHub (or upload).
- Go to the Actions tab → run the workflow (or push to main).
- Download the artifacts:
  - `app-debug-apk` (no signing, easy to install)
  - `app-release-apk` (signed, if you added secrets)
