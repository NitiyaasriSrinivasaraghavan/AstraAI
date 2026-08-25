# Astra AI - Implementation Plan

Continue the development of Astra AI, focusing on real resume analysis, ATS scoring, and AI-driven career intelligence.

## User Review Required

> [!IMPORTANT]
> **Gemini API Key Required**: The implementation uses Google's Generative AI (Gemini). A placeholder key is used in `GeminiService.kt`. The user must replace it with a valid API key from [Google AI Studio](https://aistudio.google.com/).

> [!WARNING]
> **PDF and DOCX Parsing**: PDF parsing is implemented using `pdfbox-android`. DOCX parsing is implemented by extracting the `document.xml` from the zip structure to keep dependencies light.

## Proposed Changes

### Core Infrastructure

#### [NEW] [GeminiService](file:///C:/Users/dhanr/AndroidStudioProjects/AIDrivenCompetencyPlatform/app/src/main/java/com/example/aidrivencompetencyplatform/data/GeminiService.kt)
Service class to interact with Google Gemini Pro. Handles prompt engineering for resume analysis, skill extraction, and mock interviews.

#### [NEW] [ResumeParser](file:///C:/Users/dhanr/AndroidStudioProjects/AIDrivenCompetencyPlatform/app/src/main/java/com/example/aidrivencompetencyplatform/data/ResumeParser.kt)
Utility to extract text from PDF and DOCX files using `ContentResolver`.

### Data Models

#### [MODIFY] [AppModels](file:///C:/Users/dhanr/AndroidStudioProjects/AIDrivenCompetencyPlatform/app/src/main/java/com/example/aidrivencompetencyplatform/model/AppModels.kt)
Expand models to include structured resume data, ATS categories, job roles, interview questions, and detailed roadmaps.

### ViewModels

#### [MODIFY] [AppViewModels](file:///C:/Users/dhanr/AndroidStudioProjects/AIDrivenCompetencyPlatform/app/src/main/java/com/example/aidrivencompetencyplatform/viewmodel/AppViewModels.kt)
Update ViewModels to handle real data flows:
- `DashboardViewModel`: Fetch real profile strength and matching roles.
- `ResumeViewModel`: Trigger real parsing and AI analysis.
- `SkillGapViewModel`: Calculate gaps based on target roles.
- `AiAssistantViewModel`: Use Gemini for context-aware chat.

### UI Screens

#### [MODIFY] [MainScreens](file:///C:/Users/dhanr/AndroidStudioProjects/AIDrivenCompetencyPlatform/app/src/main/java/com/example/aidrivencompetencyplatform/ui/screens/MainScreens.kt)
- Fix the "Guest" display by removing redundant UI-level persistence logic.
- Update `DashboardScreen` with real scores and insights.
- Update `ResumeUploadScreen` to handle file URIs and show detailed ATS breakdown.
- Implement missing stubs: `Profile`, `Settings` (with logout), `Job Roles`, `Interview Prep`.

## Verification Plan

### Automated Tests
- Unit tests for `ResumeParser` (extracting text from sample strings).
- Mock AI response validation in `ResumeViewModel`.

### Manual Verification
1. **Signup/Login Flow**:
   - Sign up as "Dhanushree".
   - Confirm Dashboard says "Welcome back, Dhanushree".
   - Logout and login again to verify persistence.
2. **Resume Analysis Flow**:
   - Upload a real PDF resume.
   - Verify text extraction (logcat check).
   - Verify Gemini returns structured JSON analysis.
   - Confirm scores and strengths/weaknesses are displayed.
3. **Skill Gap/Roadmap**:
   - Select "Java Backend Developer" as target role.
   - Verify matching/missing skills are calculated.
4. **AI Assistant**:
   - Ask "Why is my ATS score low?" and verify it uses current resume context.
