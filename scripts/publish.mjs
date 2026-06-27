import { openAsBlob } from 'fs'
import fs from 'fs/promises'
import path from 'path'

const API_URL = 'https://apps.bludood.com/api'
const APP_ID = process.env.BLUAPPS_APP_ID
const TOKEN = process.env.BLUAPPS_TOKEN

const PLATFORM_PATHS = {
  android: {
    dir: 'app/build/outputs/apk/release',
    file: /^app-release.apk$/
  }
}

const buildGradle = 'app/build.gradle.kts'

async function getVersion(v) {
  const res = await fetch(`${API_URL}/console/apps/${APP_ID}/versions`, {
    headers: {
      Authorization: `token ${TOKEN}`
    }
  })
  if (res.status !== 200) return console.log('Failed to get versions')
  const data = await res.json()

  const version = data.find(d => d.version === v)
  if (!version) return console.log('Failed to find version')

  return version
}

async function createVersion(v) {
  const res = await fetch(`${API_URL}/console/apps/${APP_ID}/versions`, {
    method: 'POST',
    headers: {
      Authorization: `token ${TOKEN}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      version: v
    })
  })

  const data = await res.json()
  return data
}

async function getExistingArtifacts(v) {
  const res = await fetch(
    `${API_URL}/console/apps/${APP_ID}/versions/${v}/artifacts`,
    {
      headers: {
        Authorization: `token ${TOKEN}`
      }
    }
  )

  if (res.status !== 200) return console.log('Failed to get artifacts')
  const data = await res.json()
  return data.map(d => d.platform)
}

async function uploadArtifact(v, p, a) {
  const formdata = new FormData()
  formdata.append('platform', p)
  formdata.append('data', await openAsBlob(a), path.basename(a))

  const res = await fetch(
    `${API_URL}/console/apps/${APP_ID}/versions/${v}/artifacts`,
    {
      method: 'POST',
      headers: {
        Authorization: `token ${TOKEN}`
      },
      body: formdata
    }
  )

  if (res.status !== 201) return console.log('Failed to upload artifact')
  const data = await res.json()
  return data
}

async function getCurrentVersion() {
  try {
    const buildFile = await fs.readFile(buildGradle, 'utf8')
    const existingVersion = buildFile.match(/versionName\s=\s+"([^"]+)"/)
    return existingVersion?.[1] ?? null
  } catch (error) {
    console.log(error)
    return null
  }
}

async function checkPaths() {
  const paths = Object.entries(PLATFORM_PATHS)
  const platforms = {}

  for (const [platform, { dir, file }] of paths) {
    try {
      const files = await fs.readdir(dir)
      const artifacts = files.filter(f => file.test(f))
      if (artifacts.length > 0) {
        platforms[platform] = path.join(dir, artifacts[0])
      }
    } catch {}
  }

  return platforms
}

async function publish() {
  const currentVersion = await getCurrentVersion()
  if (!currentVersion) return console.log('Failed to get current version')
  console.log('Current version:', currentVersion)

  let version = await getVersion(currentVersion)
  if (!version) {
    version = await createVersion(currentVersion)
    console.log('Created version:', version.version)
  }

  const existing = await getExistingArtifacts(version.id)
  console.log('Existing artifacts:', existing)

  const platforms = await checkPaths()
  console.log('Platforms detected:', Object.keys(platforms))

  for (const [platform, artifact] of Object.entries(platforms)) {
    if (existing.includes(platform)) {
      console.log('Skipping existing artifact:', platform)
      continue
    }
    console.log('Uploading artifact:', artifact)
    await uploadArtifact(version.id, platform, artifact)
    console.log('Artifact uploaded')
  }
}

publish()
