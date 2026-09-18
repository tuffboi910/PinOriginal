# PinOriginal

PinOriginal is a tiny Android app for downloading the highest-resolution real image Pinterest exposes for a public pin.

It does not upscale, screenshot, re-encode, or fake quality. If Pinterest only exposes a small copy, the app says that.

## How extraction works

1. Accept a Pinterest URL from the text box or Android share sheet.
2. Normalize the URL and only allow expected HTTPS Pinterest hosts.
3. Resolve redirects, including `pin.it` short links, until a canonical pin URL is reached.
4. Fetch the public pin page.
5. Parse Pinterest JSON metadata and enumerate exposed `i.pinimg.com` image variants.
6. For each exposed CDN image, derive a possible `/originals/aa/bb/cc/file.ext` URL only when the existing CDN path contains the normal Pinterest hash layout.
7. Validate every derived original candidate with HTTP:
   - status must be successful
   - `Content-Type` must be `image/*`
   - dimensions must decode as a real image
8. Rank candidates by:
   - largest pixel area
   - verified original status when dimensions tie
   - image format quality signal
   - file size as a secondary signal
9. Save the selected HTTP response bytes directly through MediaStore into `Pictures/PinOriginal/`.

## Verified original

The app only labels an image `VERIFIED ORIGINAL` when the derived Pinterest CDN original URL responds successfully as a real image and its dimensions can be read. The app does not trust URL wording alone.

If no original candidate validates, the selected image is labeled `HIGHEST PINTEREST COPY`.

## Limits

Pinterest changes page structures often. The parser is isolated in `PinterestExtractor` so it can be updated without rewriting the app.

Video-only pins are not treated as image downloads. The app tells the user that the pin contains video instead of silently saving a thumbnail.

## Build

### GitHub Actions

Push the project to a GitHub repository, open the **Actions** tab, and run **Android build**. The workflow runs unit tests and publishes `PinOriginal-debug-apk`, which contains the installable `app-debug.apk`.

### Local

```bash
./gradlew :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```
