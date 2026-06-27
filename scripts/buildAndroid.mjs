import { execSync } from 'child_process'
import fs from 'fs/promises'
import path from 'path'

const root = process.env.GITHUB_WORKSPACE ?? process.cwd()

const ANDROID_ROOT = path.join(root)
const SIGNED = true

const ANDROID_KEYSTORE_BASE64 = process.env.ANDROID_KEYSTORE_BASE64
const ANDROID_KEYSTORE_PASSWORD = process.env.ANDROID_KEYSTORE_PASSWORD
const ANDROID_KEY_ALIAS = process.env.ANDROID_KEY_ALIAS
const ANDROID_KEY_PASSWORD = process.env.ANDROID_KEY_PASSWORD

async function main() {
  const keystorePath = path.join(process.cwd(), 'keystore.jks')
  await fs.writeFile(
    keystorePath,
    Buffer.from(ANDROID_KEYSTORE_BASE64, 'base64')
  )

  execSync('chmod +x ./gradlew', {
    cwd: ANDROID_ROOT
  })

  const gradleArgs = ['assembleRelease']

  if (SIGNED === true) {
    gradleArgs.push(
      `-Pandroid.injected.signing.store.file=${keystorePath}`
    )
    gradleArgs.push(
      `-Pandroid.injected.signing.store.password=${ANDROID_KEYSTORE_PASSWORD}`
    )
    gradleArgs.push(
      `-Pandroid.injected.signing.key.alias=${ANDROID_KEY_ALIAS}`
    )
    gradleArgs.push(
      `-Pandroid.injected.signing.key.password=${ANDROID_KEY_PASSWORD}`
    )
  }

  execSync(`./gradlew ${gradleArgs.join(' ')}`, {
    cwd: ANDROID_ROOT,
    stdio: 'inherit'
  })
}

main()
