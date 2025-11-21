# ainotes

## On-device AI summaries

The app can optionally generate on-device summaries for both note titles and note bodies using ML Kit's GenAI Summarization API. Here is what to expect and how to verify the feature end-to-end:

### Prerequisites

- Android 8.0 (API 26) or higher.
- The Google AICore app present and initialized on the device (required by ML Kit's GenAI APIs).
- A network connection on first run so AICore and the summarization adapter can download if needed.

### Expected behavior

- Two toggles exist in **Settings**: **AI title suggestions** and **AI note summaries**.
- When **AI title suggestions** is on, saving a transcription automatically fills an AI-generated title; you can still edit it before saving.
- When **AI note summaries** is on, saved notes store an AI-generated summary of the note body, shown as a preview in the note list and in a dedicated summary section on the note detail screen.
- If the on-device model needs to download, the first summarization call may take longer; subsequent calls should be fast and offline-capable.

### How to verify it works

1. Build and install the app on a device that meets the prerequisites.
2. Open **Settings** and toggle **AI title suggestions** and **AI note summaries** on; these preferences persist via DataStore and sync to Firestore.
3. Record or transcribe a note with a few sentences, then save it.
4. In the note list, confirm a concise AI summary appears under the note preview when the summary toggle is enabled.
5. Open the note detail screen and confirm the **AI Summary** section is populated.
6. Turn either toggle off and repeat the flow to verify that titles or summaries are no longer auto-generated.
7. Optionally, disable network and retry summarization after the first download to confirm offline behavior.

### Troubleshooting

- If summarization fails immediately after device setup, wait for AICore initialization and retry.
- Devices with unlocked bootloaders are not supported by the ML Kit GenAI APIs.
