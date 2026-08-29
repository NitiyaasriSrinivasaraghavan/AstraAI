import { AtsScoreResult, SkillGapItem, JdMatchItem } from './types';

export const SAMPLE_RESUME_TEXT = `Alex Johnson
Email: alex.johnson@example.com | Phone: +1 (555) 382-9910 | Location: San Francisco, CA
LinkedIn: linkedin.com/in/alexjohnson-dev | GitHub: github.com/alexj-android

PROFESSIONAL SUMMARY
Experienced Senior Android & Mobile Engineer with 5+ years of production expertise architecting performant, reactive Android applications using Kotlin, Jetpack Compose, Coroutines, Flow, MVVM/MVI, and Dagger Hilt. Proven track record driving 35% improvements in app launch speed and implementing robust REST & GraphQL client architectures with Room database local caching.

TECHNICAL COMPETENCIES
- Languages: Kotlin, Java, TypeScript, SQL, Python
- Android Ecosystem: Jetpack Compose, Coroutines, Flow, ViewModel, LiveData, WorkManager, Navigation Component, Room DB, Retrofit, OkHttp, Coil
- Architecture & Patterns: Clean Architecture, MVVM, MVI, Dependency Injection (Dagger Hilt, Koin), Modularization
- Testing & CI/CD: JUnit5, MockK, Espresso, GitHub Actions, Fastlane, Firebase App Distribution
- Cloud & Tools: Firebase Suite, Google Cloud Platform, Git, Docker, Android Studio Profiler, Gradle Kotlin DSL

PROFESSIONAL EXPERIENCE
Senior Android Engineer | Nexus Technologies, San Francisco, CA
March 2022 - Present
- Led complete architectural redesign from legacy XML Views to 100% Jetpack Compose with declarative reactive UI state, reducing screen rendering time by 28%.
- Implemented offline-first synchronization engine using Room DB, Kotlin Coroutines, and WorkManager handling 250k+ daily transactions.
- Automated CI/CD build matrix using GitHub Actions and Fastlane, cutting deployment cycle times by 40%.
- Mentored 4 junior and mid-level engineers in Compose state hoisting and memory leak profiling.

Android Developer | Vertex Mobile Solutions, Austin, TX
July 2019 - February 2022
- Developed core e-commerce features with Retrofit REST client integration, handling 50k+ active shoppers.
- Integrated biometrics authentication and Android Keystore secure storage for high-security payments.
- Enhanced crash-free sessions from 97.2% to 99.8% by resolving memory leaks using LeakCanary.

EDUCATION
Bachelor of Science in Computer Science | University of California, Berkeley
Graduated: May 2019

PROJECTS
- DomainDigest (Open Source): Modular Android news aggregator app built with Compose, Material 3, and Dagger Hilt.
- PalmTrack: AI-powered habit and offline productivity app with Room persistence and local biometric encryption.

CERTIFICATIONS
- Associate Android Developer (Google Certified)
`;

export function parseResumeCandidateInfo(text: string): { name: string; email: string; phone: string; location: string } {
  const lines = text.split('\n').map(l => l.trim()).filter(l => l.length > 0);
  
  // Email
  const emailMatch = text.match(/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/);
  const email = emailMatch ? emailMatch[0].trim() : "alex.johnson@example.com";
  
  // Phone
  const phoneMatch = text.match(/(?:\+?91[\s-]?)?[6-9]\d{4}[\s-]?\d{5}|(?:\+?91[\s-]?)?[6-9]\d{9}|(?:\+?\d{1,3}[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}/);
  const phone = phoneMatch ? phoneMatch[0].trim() : "+1 (555) 382-9910";

  // Location Keywords
  const locationKeywords = [
    "srirangam", "trichy", "tiruchirappalli", "chennai", "coimbatore", "madurai", "salem",
    "bangalore", "bengaluru", "hyderabad", "pune", "mumbai", "delhi", "noida", "gurgaon",
    "kolkata", "kochi", "san francisco", "new york", "london", "seattle", "austin", "tamil nadu", "california", "texas"
  ];

  let location = "San Francisco, CA";
  for (const line of lines.slice(0, 15)) {
    const lower = line.toLowerCase();
    if (locationKeywords.some(k => lower.includes(k)) || lower.startsWith("location:")) {
      const cleaned = line.replace(/^location\s*:\s*/i, '').trim();
      const parts = cleaned.split(/[|•·,]/).map(p => p.trim());
      const locPart = parts.find(p => locationKeywords.some(k => p.toLowerCase().includes(k)));
      if (locPart && locPart.length < 50 && !locPart.includes('@') && !/\d/.test(locPart)) {
        location = locPart;
        break;
      }
    }
  }

  // Name extraction (strictly preventing location from sticking to candidate name)
  let name = "Alex Johnson";
  const blacklist = ["developer", "engineer", "designer", "architect", "analyst", "resume", "summary", "experience", "education", "skills"];
  
  for (const line of lines.slice(0, 10)) {
    const lower = line.toLowerCase();
    if (emailMatch && line.includes(emailMatch[0])) continue;
    if (phoneMatch && line.includes(phoneMatch[0])) continue;
    if (line.includes('@') || line.includes('http') || line.includes('www.') || line.includes('.com')) continue;
    if (/\d/.test(line)) continue;
    if (blacklist.some(b => lower.includes(b))) continue;

    // Check if delimited by comma or pipe
    const lineParts = line.split(/[|,•·\-]/).map(p => p.trim()).filter(p => p.length > 0);
    let candidatePart = line;
    if (lineParts.length > 1) {
      candidatePart = lineParts.find(p => !locationKeywords.some(k => p.toLowerCase().includes(k)) && !blacklist.some(b => p.toLowerCase().includes(b))) || lineParts[0];
    } else if (line.includes(',')) {
      const commaParts = line.split(',').map(p => p.trim());
      candidatePart = commaParts[0];
    }

    let cleanedCandidate = candidatePart;
    for (const k of locationKeywords) {
      cleanedCandidate = cleanedCandidate.replace(new RegExp(`\\b${k}\\b`, 'gi'), '').trim();
    }
    cleanedCandidate = cleanedCandidate.replace(/[,|•·\-]+$/, '').trim();

    const words = cleanedCandidate.split(/\s+/).filter(w => w.length > 0);
    if (words.length >= 1 && words.length <= 4 && words.every(w => /^[A-Za-z.]+$/.test(w))) {
      name = cleanedCandidate;
      break;
    }
  }

  return { name, email, phone, location };
}

export function generateDeterministicAtsScore(targetRole: string = "Android Developer", rawText: string = SAMPLE_RESUME_TEXT): AtsScoreResult {
  const candidateInfo = parseResumeCandidateInfo(rawText);
  return {
    overallScore: 88,
    candidateName: candidateInfo.name,
    candidateEmail: candidateInfo.email,
    candidatePhone: candidateInfo.phone,
    candidateLocation: candidateInfo.location,
    targetRole: targetRole,
    keywordCoverage: {
      metricName: "Keyword Coverage",
      score: 85,
      maxScore: 100,
      weightPercentage: 35,
      formula: "Keyword Score = (Detected Crucial Keywords / Required Benchmark Keywords) × 100",
      formulaDescription: "Evaluates exact match and contextual coverage of core programming languages, frameworks, architecture patterns, and domain competencies required for the target role.",
      arithmeticComputation: "(17 matched keywords / 20 required role keywords) × 100 = 85.0%",
      evidenceFound: [
        "Kotlin (Found 5x in Summary, Skills, Experience)",
        "Jetpack Compose (Found 4x across Experience & Projects)",
        "Coroutines & Flow (Found 3x in Experience)",
        "MVVM / MVI Architecture (Explicitly stated in Summary)",
        "Dagger Hilt & Dependency Injection (Detected in Skills)",
        "Room DB & SQLite caching (Detected in Experience)",
        "Retrofit & REST API integration (Found in Experience)"
      ],
      improvementSuggestion: "Include specific mentions of NDK, C++, or Jetpack Glance to reach 95%+ match."
    },
    resumeStructure: {
      metricName: "Resume Structure",
      score: 92,
      maxScore: 100,
      weightPercentage: 25,
      formula: "Structure Score = (Identified Standard Sections × 10) + Chronological Flow Points",
      formulaDescription: "Assesses chronological order, standard industry heading labels, reverse chronological history, and standardized header metadata.",
      arithmeticComputation: "(8 standard sections detected × 10) + 12 ordering & date points = 92.0%",
      evidenceFound: [
        "Contact Information (Header formatted cleanly)",
        "Professional Summary (Detected with clear role alignment)",
        "Technical Competencies (Categorized cleanly by domain)",
        "Professional Experience (Reverse chronological format)",
        "Education (Accredited BS degree with graduation date)",
        "Projects & Certifications (Verified sections)"
      ],
      improvementSuggestion: "Add concise metric bullet points for earlier roles."
    },
    formattingSafety: {
      metricName: "Formatting Safety",
      score: 95,
      maxScore: 100,
      weightPercentage: 20,
      formula: "Safety Score = 100 - (Complex Tables Penalty + Non-Standard Glyphs Penalty + Multi-Column Obstacles)",
      formulaDescription: "Checks for clean single-column readability, standard unicode glyphs, absence of nested tables or image-based text blocks that trip ATS parsers.",
      arithmeticComputation: "100 - (0 tables - 5 non-standard bullet characters) = 95.0%",
      evidenceFound: [
        "Single column flow ensures universal scanner compatibility",
        "Standard typography and line spacing",
        "Clear bullet point symbols compatible with Taleo & Workday",
        "Zero hidden text layers or graphic text rendering"
      ],
      improvementSuggestion: "Maintain standard dash or dot bullet points throughout."
    },
    parsingAccuracy: {
      metricName: "Parsing Accuracy",
      score: 90,
      maxScore: 100,
      weightPercentage: 20,
      formula: "Parsing Score = (Successfully Extracted Data Entities / Total Expected Entities) × 100",
      formulaDescription: "Verifies that names, contact numbers, email strings, job titles, date spans, and institution names are cleanly extracted into standard JSON schemas.",
      arithmeticComputation: "(9 verified entities / 10 expected entities) × 100 = 90.0%",
      evidenceFound: [
        "Full Name: Alex Johnson (100% confidence)",
        "Email Address: alex.johnson@example.com (Valid regex)",
        "Phone: +1 (555) 382-9910 (E.164 verified)",
        "Location: San Francisco, CA (Geo parsed)",
        "2 employment blocks with accurate date intervals"
      ],
      improvementSuggestion: "Include country code explicitly in contact section."
    },
    strengths: [
      "Exceptional coverage of modern Kotlin & Jetpack Compose declarative UI paradigms",
      "Robust evidence of Clean Architecture and offline-first Room database synchronization",
      "High ATS readability index with standard headings and clean single-column structure"
    ],
    weaknesses: [
      "Could add more quantified metrics to the secondary project descriptions",
      "No direct evidence found for low-level memory allocation profiling or NDK C++ integration"
    ],
    priorityFixes: [
      "Quantify impact in Project section (e.g., 'achieved 4.8★ app rating across 10k users')",
      "Highlight automated unit test coverage percentage in CI/CD pipeline",
      "Add explicit certifications or links to published Google Play Store APKs"
    ]
  };
}

export const SAMPLE_SKILL_GAPS: SkillGapItem[] = [
  { name: "Kotlin & Coroutines", category: "Languages", status: "VERIFIED", matchPercent: 100, evidence: "5+ years production experience, reactive state management", actionItem: "Maintain core mastery" },
  { name: "Jetpack Compose", category: "Frameworks", status: "VERIFIED", matchPercent: 95, evidence: "Architected 100% Compose rewrite, state hoisting, custom layouts", actionItem: "Explore Compose Multiplatform" },
  { name: "Room & SQLite Offline Cache", category: "Architecture", status: "VERIFIED", matchPercent: 90, evidence: "Built offline synchronization engine for 250k transactions", actionItem: "Deepen multi-table relational migrations" },
  { name: "Dagger Hilt & Dependency Injection", category: "Frameworks", status: "VERIFIED", matchPercent: 92, evidence: "Configured multi-module Hilt dependency graphs", actionItem: "Review Koin / Kotlin Inject comparisons" },
  { name: "Android NDK & C++ Layer", category: "Languages", status: "MISSING", matchPercent: 20, evidence: "No evidence found in resume text", actionItem: "Create JNI sample binding for audio or image manipulation" },
  { name: "Jetpack Glance & App Widgets", category: "Frameworks", status: "PARTIAL", matchPercent: 55, evidence: "General Compose knowledge detected, no Glance specific project", actionItem: "Build an interactive Compose glance widget" },
  { name: "CI/CD & Fastlane Automation", category: "Cloud & Tools", status: "VERIFIED", matchPercent: 88, evidence: "Automated GitHub Actions and Fastlane pipeline", actionItem: "Add Play Store deployment automation script" },
  { name: "Performance Profiling (Systrace/LeakCanary)", category: "Architecture", status: "VERIFIED", matchPercent: 85, evidence: "Reduced screen render time by 28% and fixed memory leaks", actionItem: "Document Macrobenchmark baseline profiles" }
];

export const SAMPLE_JD_MATCHES: JdMatchItem[] = [
  { requirement: "5+ years professional experience building Android apps in Kotlin", importance: "HIGH", matched: true, notes: "Resume documents 5+ years across Nexus & Vertex Mobile" },
  { requirement: "Deep expertise in Jetpack Compose, StateFlow, and Coroutines", importance: "HIGH", matched: true, notes: "Strongly demonstrated across both professional experience blocks" },
  { requirement: "Experience with Clean Architecture, MVVM/MVI, and Dagger Hilt", importance: "HIGH", matched: true, notes: "Explicitly highlighted with architectural leadership" },
  { requirement: "Familiarity with CI/CD tools (GitHub Actions, Fastlane)", importance: "MEDIUM", matched: true, notes: "Built automated build matrix saving 40% cycle time" },
  { requirement: "Knowledge of Android NDK and C++ integrations", importance: "NICE_TO_HAVE", matched: false, notes: "No direct mentions in current resume version" }
];
