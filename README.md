# Prompt Studio

Prompt Studio is an unofficial, open source Android client for the Higgsfield API. It lets you create images and videos, manage conversations, and work with model-specific settings and media references. The app has no ads, subscriptions, or in-app purchases. It is independently developed and is not affiliated with or endorsed by Higgsfield. API usage may incur charges on your own provider account.

## Features

- Separate image and video workspaces with model selection and model-specific controls.
- Local conversations, drafts, and a Creative Brief for reusable creative direction.
- Image and video references where supported by the selected model.
- Generation progress, results, downloads, and error details.
- Light and dark themes.

Drafts and conversation history are stored locally. Media is uploaded and generation requests are sent when you submit a request.

## Setup

1. Copy `secrets.properties.example` to `app/secrets/secrets.properties`.
2. Set `HF_KEY_ID` and `HF_KEY_SECRET` using credentials from your own Higgsfield API account.
3. Build with `./gradlew :app:assembleDebug` (or `gradlew.bat :app:assembleDebug` on Windows).

The app can run without credentials, but generation requests will fail authentication. Keep your secrets file out of version control. Do not distribute an APK built with your own API keys: values embedded in an APK can be extracted.

## Pricing information

The provider does not currently offer an API for live model rates. Starting prices shown in the app are manually maintained, may be outdated, and are informational only. Check current pricing in your provider account before submitting a billable request.

## Documentation

The [architecture guide](docs/architecture.md) explains storage, uploads, request handling, and the app structure. [Future workflow API work](docs/workflow-api-future.md) records guided workflows that are not yet exposed in the app.

## Screenshots

<table>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/91cc3bf2-00d0-49c7-84bd-c7fd3d4ba4a8" alt="App screenshot 1" width="320" /></td>
    <td><img src="https://github.com/user-attachments/assets/abd59999-3fb3-40c5-9e01-2b9cdce06aef" alt="App screenshot 2" width="320" /></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/152ae76d-0600-4ca5-b1f4-8b249b9662cd" alt="App screenshot 3" width="320" /></td>
    <td><img src="https://github.com/user-attachments/assets/acd5de22-742d-4e82-8757-786fc56b9857" alt="App screenshot 4" width="320" /></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/64a7a74b-b20c-48bd-9e4e-06a6930ce9d6" alt="App screenshot 5" width="320" /></td>
    <td><img src="https://github.com/user-attachments/assets/84e53a9e-a1ec-4945-af56-58422c2dc8e3" alt="App screenshot 6" width="320" /></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/6ce03327-30ee-47e9-a8f4-75ebdd834980" alt="App screenshot 7" width="320" /></td>
    <td><img src="https://github.com/user-attachments/assets/25bf8ee1-c879-4ce0-84b4-bf08665a3842" alt="App screenshot 8" width="320" /></td>
  </tr>
</table>

## License

The source code is licensed under [Apache License 2.0](LICENSE). Model names, API documentation, trademarks, and third-party artwork remain the property of their respective owners.
