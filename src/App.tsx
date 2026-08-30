import React, { useState, useEffect } from 'react';
import { 
  FileText, CheckCircle2, AlertCircle, ShieldAlert, Sparkles, 
  ArrowRight, Search, Zap, Check, ChevronRight, X, Cpu, 
  BarChart3, Target, BookOpen, MessageSquare, Award, RefreshCw
} from 'lucide-react';
import { SAMPLE_RESUME_TEXT, generateDeterministicAtsScore, SAMPLE_SKILL_GAPS, SAMPLE_JD_MATCHES } from './sampleData';
import { AtsScoreResult, AtsCalculation } from './types';

export default function App() {
  const [activeTab, setActiveTab] = useState<'upload' | 'ats' | 'skillgap' | 'jd' | 'ai'>('ats');
  const [targetRole, setTargetRole] = useState('Senior Android Engineer');
  const [resumeText, setResumeText] = useState(SAMPLE_RESUME_TEXT);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [analysisStep, setAnalysisStep] = useState(0);
  
  // Real-time analysis module status state
  const [atsComplete, setAtsComplete] = useState(true);
  const [skillGapComplete, setSkillGapComplete] = useState(true);
  const [jdMatchingComplete, setJdMatchingComplete] = useState(true);

  // Score state
  const [atsData, setAtsData] = useState<AtsScoreResult>(() => generateDeterministicAtsScore(targetRole));
  
  // Dynamic Score Loading Animation
  const [animatedOverallScore, setAnimatedOverallScore] = useState(0);

  useEffect(() => {
    let start = 0;
    const end = atsData.overallScore;
    const duration = 1200;
    const increment = end / (duration / 25);

    const timer = setInterval(() => {
      start += increment;
      if (start >= end) {
        setAnimatedOverallScore(end);
        clearInterval(timer);
      } else {
        setAnimatedOverallScore(Math.floor(start));
      }
    }, 25);

    return () => clearInterval(timer);
  }, [atsData.overallScore, activeTab]);

  // "Why?" Modal calculation metric state
  const [selectedWhyMetric, setSelectedWhyMetric] = useState<AtsCalculation | null>(null);

  // Trigger analysis simulation
  const startRealtimeAnalysis = () => {
    setIsAnalyzing(true);
    setAnalysisStep(0);
    setAtsComplete(false);
    setSkillGapComplete(false);
    setJdMatchingComplete(false);

    // Step 1: ATS Analysis
    setTimeout(() => {
      setAtsComplete(true);
      setAnalysisStep(1);
    }, 1200);

    // Step 2: Skill Gap Analysis
    setTimeout(() => {
      setSkillGapComplete(true);
      setAnalysisStep(2);
    }, 2400);

    // Step 3: JD Matching
    setTimeout(() => {
      setJdMatchingComplete(true);
      setAnalysisStep(3);
    }, 3600);

    // Final finish and jump to ATS dashboard
    setTimeout(() => {
      setIsAnalyzing(false);
      setAtsData(generateDeterministicAtsScore(targetRole, resumeText));
      setActiveTab('ats');
    }, 4400);
  };

  return (
    <div className="min-h-screen bg-[#F8FAF8] text-[#1E293B] flex flex-col selection:bg-[#C8E6C9]">
      {/* Top Navigation Bar */}
      <header className="sticky top-0 z-30 bg-white/90 backdrop-blur-md border-b border-[#E2E8F0] shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-[#1B5E20] flex items-center justify-center text-white shadow-sm shadow-green-900/20">
              <Sparkles className="w-5 h-5 text-[#81C784]" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-bold text-lg text-[#0D3B12] tracking-tight">NoviQ</span>
                <span className="text-[10px] uppercase font-bold tracking-widest px-2 py-0.5 rounded-full bg-[#E8F5E9] text-[#1B5E20] border border-[#C8E6C9]">
                  Competency OS
                </span>
              </div>
              <p className="text-xs text-slate-500">ATS Diagnostics & Skill Gap Benchmarks</p>
            </div>
          </div>

          <div className="flex items-center space-x-1 sm:space-x-2">
            <button
              onClick={() => setActiveTab('upload')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-all ${
                activeTab === 'upload' ? 'bg-[#1B5E20] text-white shadow-sm' : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              Upload / Edit
            </button>
            <button
              onClick={() => setActiveTab('ats')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-all ${
                activeTab === 'ats' ? 'bg-[#1B5E20] text-white shadow-sm' : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              ATS Score & Why
            </button>
            <button
              onClick={() => setActiveTab('skillgap')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-all ${
                activeTab === 'skillgap' ? 'bg-[#1B5E20] text-white shadow-sm' : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              Skill Gap
            </button>
            <button
              onClick={() => setActiveTab('jd')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-all ${
                activeTab === 'jd' ? 'bg-[#1B5E20] text-white shadow-sm' : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              JD Match
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* ========================================================================= */}
        {/* TAB 1: UPLOAD / ANALYZE */}
        {/* ========================================================================= */}
        {activeTab === 'upload' && (
          <div className="max-w-4xl mx-auto space-y-6">
            <div className="bg-white rounded-2xl p-6 sm:p-8 border border-slate-200 shadow-sm">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-100">
                <div>
                  <h2 className="text-xl font-bold text-slate-900">Configure Resume & Role</h2>
                  <p className="text-sm text-slate-500">Provide the resume text to run real-time multi-dimensional ATS verification.</p>
                </div>
                <div className="flex items-center space-x-2">
                  <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Target Role:</span>
                  <input
                    type="text"
                    value={targetRole}
                    onChange={(e) => setTargetRole(e.target.value)}
                    className="px-3 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-sm font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#1B5E20]"
                    placeholder="e.g. Senior Android Engineer"
                  />
                </div>
              </div>

              <div className="mt-6 space-y-4">
                <div className="flex justify-between items-center">
                  <label className="text-sm font-semibold text-slate-700">Resume Plaintext</label>
                  <button
                    onClick={() => setResumeText(SAMPLE_RESUME_TEXT)}
                    className="text-xs text-[#1B5E20] font-semibold hover:underline flex items-center gap-1"
                  >
                    <RefreshCw className="w-3 h-3" /> Reset to Sample Resume
                  </button>
                </div>
                <textarea
                  rows={14}
                  value={resumeText}
                  onChange={(e) => setResumeText(e.target.value)}
                  className="w-full p-4 rounded-xl font-mono text-xs text-slate-800 bg-slate-50 border border-slate-200 focus:outline-none focus:ring-2 focus:ring-[#1B5E20] focus:bg-white transition-all leading-relaxed"
                  placeholder="Paste resume markdown or plain text here..."
                />

                <div className="pt-4 flex justify-end">
                  <button
                    onClick={startRealtimeAnalysis}
                    className="px-6 py-3.5 rounded-xl bg-[#1B5E20] hover:bg-[#0D3B12] text-white font-semibold text-sm flex items-center gap-2 shadow-md shadow-green-900/10 transition-all cursor-pointer"
                  >
                    <Sparkles className="w-4 h-4 text-[#81C784]" />
                    Run Real-Time AI Analysis (3 Modules)
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ========================================================================= */}
        {/* TAB 2: ATS DASHBOARD & WHY EXPLANATION */}
        {/* ========================================================================= */}
        {activeTab === 'ats' && (
          <div className="space-y-8">
            {/* 1. SCORE HERO (MUST APPEAR FIRST AT THE TOP) */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Overall ATS Score Card with Dynamic Score Loading */}
              <div className="bg-gradient-to-br from-[#1B5E20] to-[#0D3B12] text-white rounded-2xl p-6 flex flex-col justify-between shadow-md transform transition-all duration-300 hover:-translate-y-1.5 hover:shadow-xl cursor-pointer">
                <div>
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold uppercase tracking-widest text-[#81C784]">Overall ATS Score</span>
                    <span className="px-2.5 py-0.5 rounded-full bg-white/20 text-xs font-bold text-white backdrop-blur-sm">
                      Taleo & Workday Ready
                    </span>
                  </div>
                  <div className="mt-6 flex items-baseline gap-2">
                    <span className="text-6xl font-extrabold tracking-tight transition-all duration-500">{animatedOverallScore}</span>
                    <span className="text-xl text-[#81C784] font-bold">/ 100</span>
                  </div>
                  <div className="mt-3 w-full bg-white/10 h-2.5 rounded-full overflow-hidden">
                    <div 
                      className="bg-[#81C784] h-full rounded-full transition-all duration-1000 ease-out"
                      style={{ width: `${animatedOverallScore}%` }}
                    />
                  </div>
                  <p className="mt-3 text-xs text-green-100 leading-relaxed">
                    Weighted synthesis of 4 core deterministic scanners calibrated for modern enterprise hiring algorithms.
                  </p>
                </div>

                <div className="mt-6 pt-4 border-t border-white/15 flex items-center justify-between text-xs text-green-200">
                  <span>Match Confidence: 94.8%</span>
                  <span className="font-semibold text-white">Tier 1 Candidate</span>
                </div>
              </div>

              {/* 4 Quick Dimension Contributions */}
              <div className="lg:col-span-2 grid grid-cols-1 sm:grid-cols-2 gap-4">
                {[
                  { name: "Keyword Coverage", score: atsData.keywordCoverage.score, weight: "35%", color: "text-[#1B5E20]", metric: atsData.keywordCoverage },
                  { name: "Resume Structure", score: atsData.resumeStructure.score, weight: "25%", color: "text-[#1B5E20]", metric: atsData.resumeStructure },
                  { name: "Formatting Safety", score: atsData.formattingSafety.score, weight: "20%", color: "text-[#1B5E20]", metric: atsData.formattingSafety },
                  { name: "Parsing Accuracy", score: atsData.parsingAccuracy.score, weight: "20%", color: "text-[#1B5E20]", metric: atsData.parsingAccuracy }
                ].map((item, idx) => (
                  <div 
                    key={idx}
                    onClick={() => setSelectedWhyMetric(item.metric)}
                    className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between transform transition-all duration-300 hover:-translate-y-1.5 hover:shadow-lg hover:border-emerald-300 cursor-pointer"
                  >
                    <div>
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold text-slate-500">{item.name}</span>
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">Weight {item.weight}</span>
                      </div>
                      <div className="mt-2 flex items-baseline gap-1">
                        <span className={`text-3xl font-extrabold ${item.color}`}>{item.score}%</span>
                        <span className="text-xs text-slate-400 font-semibold">/ 100</span>
                      </div>
                    </div>
                    <div className="mt-3 pt-2 border-t border-slate-100 flex items-center justify-between text-xs text-[#1B5E20] font-semibold">
                      <span>View Formula & Why</span>
                      <ChevronRight className="w-3.5 h-3.5" />
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* 2. ENTITY DATA EXTRACTION DIAGNOSTICS (MUST APPEAR SECOND) */}
            <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm space-y-4 transform transition-all duration-300 hover:-translate-y-1 hover:shadow-md cursor-pointer">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-4 border-b border-slate-100">
                <div className="flex items-center space-x-3">
                  <div className="w-9 h-9 rounded-xl bg-[#E8F5E9] text-[#1B5E20] flex items-center justify-center font-bold">
                    <Sparkles className="w-5 h-5 text-[#2E7D32]" />
                  </div>
                  <div>
                    <h3 className="font-bold text-base text-slate-900">Entity Data Extraction Diagnostics</h3>
                    <p className="text-xs text-slate-500">Exact resume metadata extracted by the ATS parser engine.</p>
                  </div>
                </div>
                <span className="text-xs font-semibold px-3 py-1 bg-emerald-50 text-[#1B5E20] rounded-full border border-[#C8E6C9] self-start sm:self-auto">
                  ✓ High-Confidence Parser Detection
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3">
                <div className="bg-slate-50 rounded-xl p-3 border border-slate-200/80">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Candidate Name</span>
                  <p className="text-sm font-bold text-slate-900 mt-0.5 truncate">{atsData.candidateName || "Not detected"}</p>
                </div>
                <div className="bg-slate-50 rounded-xl p-3 border border-slate-200/80">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Email Address</span>
                  <p className="text-sm font-bold text-slate-900 mt-0.5 truncate">{atsData.candidateEmail || "Not detected"}</p>
                </div>
                <div className="bg-slate-50 rounded-xl p-3 border border-slate-200/80">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Phone Number</span>
                  <p className="text-sm font-bold text-slate-900 mt-0.5 truncate">{atsData.candidatePhone || "Not detected"}</p>
                </div>
                <div className="bg-slate-50 rounded-xl p-3 border border-slate-200/80">
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Location</span>
                  <p className="text-sm font-bold text-slate-900 mt-0.5 truncate">{atsData.candidateLocation || "Not detected"}</p>
                </div>
              </div>
            </div>

            {/* 3. SUB-FEATURES OF ATS */}
            {/* Candidate Target Position & Profile Sub-feature Header */}
            <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4 transform transition-all duration-300 hover:-translate-y-1 hover:shadow-md">
              <div className="flex items-center space-x-4">
                <div className="w-12 h-12 rounded-2xl bg-[#E8F5E9] border border-[#C8E6C9] flex items-center justify-center text-[#1B5E20] font-bold text-lg">
                  {atsData.candidateName.split(' ').map(n => n[0]).join('')}
                </div>
                <div>
                  <h2 className="text-base font-bold text-slate-900">{atsData.candidateName}</h2>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Target Role: <span className="font-semibold text-slate-800">{atsData.targetRole}</span>
                  </p>
                </div>
              </div>

              <div className="flex items-center space-x-3">
                <button
                  onClick={startRealtimeAnalysis}
                  className="px-4 py-2 rounded-xl bg-[#1B5E20] text-white text-xs font-semibold hover:bg-[#0D3B12] transition-all flex items-center gap-1.5 shadow-sm"
                >
                  <RefreshCw className="w-3.5 h-3.5" /> Re-Analyze
                </button>
              </div>
            </div>

            {/* Strengths & Immediate Priority Boosters */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between transform transition-all duration-300 hover:-translate-y-1.5 hover:shadow-lg cursor-pointer">
                <div>
                  <div className="flex items-center space-x-2 text-[#1B5E20] mb-3">
                    <CheckCircle2 className="w-5 h-5 text-[#2E7D32]" />
                    <h3 className="font-bold text-sm text-slate-900">What's Helping You Pass</h3>
                  </div>
                  <ul className="space-y-2 text-xs text-slate-600 leading-relaxed">
                    {atsData.strengths.map((str, i) => (
                      <li key={i} className="flex items-start gap-2">
                        <span className="w-1.5 h-1.5 rounded-full bg-[#2E7D32] mt-1.5 shrink-0" />
                        <span>{str}</span>
                      </li>
                    ))}
                  </ul>
                </div>
                <div className="mt-4 pt-3 border-t border-slate-100 text-[11px] font-semibold text-[#1B5E20]">
                  ✓ 3 High-Impact ATS Strengths
                </div>
              </div>

              <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between transform transition-all duration-300 hover:-translate-y-1.5 hover:shadow-lg cursor-pointer">
                <div>
                  <div className="flex items-center space-x-2 text-amber-700 mb-3">
                    <AlertCircle className="w-5 h-5 text-amber-600" />
                    <h3 className="font-bold text-sm text-slate-900">Actionable Booster Priorities</h3>
                  </div>
                  <ul className="space-y-2 text-xs text-slate-600 leading-relaxed">
                    {atsData.priorityFixes.map((fix, i) => (
                      <li key={i} className="flex items-start gap-2">
                        <span className="w-1.5 h-1.5 rounded-full bg-amber-500 mt-1.5 shrink-0" />
                        <span>{fix}</span>
                      </li>
                    ))}
                  </ul>
                </div>
                <div className="mt-4 pt-3 border-t border-slate-100 text-[11px] font-semibold text-amber-700">
                  ⚡ Implement for +8-12 ATS points
                </div>
              </div>
            </div>

            {/* 4 ATS Performance Dimensions with "Why?" Triggers */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-lg font-bold text-slate-900">4 ATS Performance Dimensions</h2>
                  <p className="text-xs text-slate-500">Tap <span className="font-bold text-[#1B5E20]">"Why?"</span> on any metric to view its exact mathematical calculation formula and evidence.</p>
                </div>
                <span className="text-xs font-semibold px-3 py-1 bg-[#E8F5E9] text-[#1B5E20] rounded-full border border-[#C8E6C9]">
                  Deterministic Calculations
                </span>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {[
                  atsData.keywordCoverage,
                  atsData.resumeStructure,
                  atsData.formattingSafety,
                  atsData.parsingAccuracy
                ].map((metric) => (
                  <div 
                    key={metric.metricName}
                    className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm hover:border-[#81C784] transform transition-all duration-300 hover:-translate-y-1.5 hover:shadow-lg flex flex-col justify-between cursor-pointer"
                  >
                    <div>
                      <div className="flex items-center justify-between mb-3">
                        <div className="flex items-center space-x-2">
                          <span className="font-bold text-sm text-slate-900">{metric.metricName}</span>
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-slate-100 text-slate-600">
                            Weight: {metric.weightPercentage}%
                          </span>
                        </div>
                        <div className="flex items-baseline space-x-1">
                          <span className="text-2xl font-bold text-[#1B5E20]">{metric.score}</span>
                          <span className="text-xs text-slate-400 font-semibold">/{metric.maxScore}</span>
                        </div>
                      </div>

                      <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden mb-3">
                        <div 
                          className="bg-[#1B5E20] h-full rounded-full transition-all duration-700"
                          style={{ width: `${(metric.score / metric.maxScore) * 100}%` }}
                        />
                      </div>

                      <p className="text-xs text-slate-600 line-clamp-2 leading-relaxed">
                        {metric.formulaDescription}
                      </p>
                    </div>

                    <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between">
                      <span className="text-[11px] text-slate-500 font-medium truncate max-w-[200px]">
                        {metric.evidenceFound.length} evidence points verified
                      </span>
                      <button
                        onClick={() => setSelectedWhyMetric(metric)}
                        className="px-3 py-1.5 rounded-lg bg-[#E8F5E9] hover:bg-[#C8E6C9] text-[#1B5E20] font-bold text-xs flex items-center gap-1 transition-colors cursor-pointer"
                      >
                        <Zap className="w-3.5 h-3.5" /> Why? (Formula & Breakdown)
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Next Steps Banner */}
            <div className="bg-[#E8F5E9] border border-[#C8E6C9] rounded-2xl p-6 flex flex-col sm:flex-row items-center justify-between gap-4 transform transition-all duration-300 hover:-translate-y-1 hover:shadow-md cursor-pointer">
              <div className="flex items-center space-x-4">
                <div className="w-12 h-12 rounded-xl bg-[#1B5E20] text-white flex items-center justify-center shrink-0">
                  <Target className="w-6 h-6 text-[#81C784]" />
                </div>
                <div>
                  <h3 className="font-bold text-slate-900 text-sm">Next Step: Inspect Skill Gap Matrix</h3>
                  <p className="text-xs text-slate-600 mt-0.5">Explore verified vs missing competencies compared against the senior role benchmark.</p>
                </div>
              </div>
              <button
                onClick={() => setActiveTab('skillgap')}
                className="px-5 py-2.5 rounded-xl bg-[#1B5E20] hover:bg-[#0D3B12] text-white font-semibold text-xs flex items-center gap-2 shrink-0 shadow-sm"
              >
                Continue to Skill Gap <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}

        {/* ========================================================================= */}
        {/* TAB 3: SKILL GAP MATRIX */}
        {/* ========================================================================= */}
        {activeTab === 'skillgap' && (
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-xl font-bold text-slate-900">Skill Gap Benchmarks</h2>
                <p className="text-sm text-slate-500">Deterministic competency classification against {atsData.targetRole} role standards.</p>
              </div>
              <div className="flex items-center space-x-2">
                <span className="text-xs font-semibold px-3 py-1 rounded-full bg-emerald-100 text-emerald-800">
                  6 Verified
                </span>
                <span className="text-xs font-semibold px-3 py-1 rounded-full bg-amber-100 text-amber-800">
                  1 Partial
                </span>
                <span className="text-xs font-semibold px-3 py-1 rounded-full bg-rose-100 text-rose-800">
                  1 Missing
                </span>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {SAMPLE_SKILL_GAPS.map((item, idx) => (
                <div key={idx} className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm space-y-3">
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">{item.category}</span>
                      <h3 className="font-bold text-sm text-slate-900">{item.name}</h3>
                    </div>
                    <span className={`text-[10px] font-extrabold px-2.5 py-1 rounded-md uppercase tracking-wider ${
                      item.status === 'VERIFIED' ? 'bg-[#E8F5E9] text-[#1B5E20] border border-[#C8E6C9]' :
                      item.status === 'PARTIAL' ? 'bg-amber-50 text-amber-700 border border-amber-200' :
                      'bg-rose-50 text-rose-700 border border-rose-200'
                    }`}>
                      {item.status} ({item.matchPercent}%)
                    </span>
                  </div>

                  <div className="bg-slate-50 p-3 rounded-xl text-xs space-y-1.5 border border-slate-100">
                    <p className="text-slate-600"><span className="font-semibold text-slate-800">Evidence:</span> {item.evidence}</p>
                    <p className="text-[#1B5E20]"><span className="font-semibold text-slate-800">Action Plan:</span> {item.actionItem}</p>
                  </div>
                </div>
              ))}
            </div>

            <div className="pt-4 flex justify-end">
              <button
                onClick={() => setActiveTab('jd')}
                className="px-5 py-2.5 rounded-xl bg-[#1B5E20] hover:bg-[#0D3B12] text-white font-semibold text-xs flex items-center gap-2 shadow-sm"
              >
                Continue to JD Matching <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}

        {/* ========================================================================= */}
        {/* TAB 4: JOB DESCRIPTION MATCHING */}
        {/* ========================================================================= */}
        {activeTab === 'jd' && (
          <div className="space-y-6">
            <div>
              <h2 className="text-xl font-bold text-slate-900">Job Description Alignment</h2>
              <p className="text-sm text-slate-500">Cross-referencing resume text against key qualifications for {atsData.targetRole}.</p>
            </div>

            <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden divide-y divide-slate-100">
              {SAMPLE_JD_MATCHES.map((jd, idx) => (
                <div key={idx} className="p-5 flex items-start justify-between gap-4">
                  <div className="flex items-start space-x-3">
                    <div className={`mt-0.5 w-6 h-6 rounded-full flex items-center justify-center shrink-0 ${
                      jd.matched ? 'bg-[#E8F5E9] text-[#1B5E20]' : 'bg-rose-50 text-rose-600'
                    }`}>
                      {jd.matched ? <Check className="w-4 h-4 stroke-[3]" /> : <X className="w-4 h-4 stroke-[3]" />}
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-slate-900">{jd.requirement}</p>
                      <p className="text-xs text-slate-500 mt-1">{jd.notes}</p>
                    </div>
                  </div>
                  <span className={`text-[10px] font-bold px-2 py-0.5 rounded-md uppercase tracking-wider shrink-0 ${
                    jd.importance === 'HIGH' ? 'bg-purple-50 text-purple-700 border border-purple-200' :
                    jd.importance === 'MEDIUM' ? 'bg-blue-50 text-blue-700 border border-blue-200' :
                    'bg-slate-100 text-slate-600'
                  }`}>
                    {jd.importance}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
      </main>

      {/* ========================================================================= */}
      {/* 3-MODULE REAL-TIME ANALYSIS MODAL */}
      {/* ========================================================================= */}
      {isAnalyzing && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-fade-in">
          <div className="bg-white w-full max-w-lg rounded-3xl p-6 sm:p-8 shadow-2xl border border-slate-100 space-y-6">
            <div className="flex items-center justify-between border-b border-slate-100 pb-4">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 rounded-xl bg-[#E8F5E9] text-[#1B5E20] flex items-center justify-center">
                  <Sparkles className="w-5 h-5 animate-pulse" />
                </div>
                <div>
                  <h3 className="font-bold text-base text-slate-900">
                    {analysisStep === 3 ? "Analysis Complete" : "Analyzing Resume..."}
                  </h3>
                  <p className="text-xs text-slate-500">Target Role: {targetRole}</p>
                </div>
              </div>
              <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-[#E8F5E9] text-[#1B5E20]">
                {analysisStep === 3 ? "3 of 3 Complete" : `${analysisStep} of 3 Modules`}
              </span>
            </div>

            {/* 3 Dedicated Modules List */}
            <div className="space-y-3">
              {/* Module 1 */}
              <div className={`p-4 rounded-2xl border transition-all flex items-start space-x-3 ${
                atsComplete ? 'bg-[#E8F5E9]/40 border-[#C8E6C9]' : 'bg-slate-50 border-slate-200'
              }`}>
                <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${
                  atsComplete ? 'bg-[#1B5E20] text-white' : 'bg-slate-200 text-slate-400'
                }`}>
                  {atsComplete ? <Check className="w-4 h-4 stroke-[3]" /> : <RefreshCw className="w-4 h-4 animate-spin text-[#1B5E20]" />}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <h4 className="font-bold text-sm text-slate-900">1. ATS Analysis</h4>
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                      atsComplete ? 'bg-[#E8F5E9] text-[#1B5E20]' : 'bg-slate-200 text-slate-600'
                    }`}>
                      {atsComplete ? 'Completed' : 'In Progress'}
                    </span>
                  </div>
                  <p className="text-xs text-slate-500 mt-0.5 leading-relaxed">
                    Analyzing resume for ATS compatibility, keywords, structure, formatting, and parsing.
                  </p>
                  {atsComplete && (
                    <span className="inline-block mt-2 text-[11px] font-semibold text-[#1B5E20] bg-white px-2 py-0.5 rounded border border-[#C8E6C9]">
                      ✓ ATS Compatibility Score: 88/100
                    </span>
                  )}
                </div>
              </div>

              {/* Module 2 */}
              <div className={`p-4 rounded-2xl border transition-all flex items-start space-x-3 ${
                skillGapComplete ? 'bg-[#E8F5E9]/40 border-[#C8E6C9]' : 'bg-slate-50 border-slate-200'
              }`}>
                <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${
                  skillGapComplete ? 'bg-[#1B5E20] text-white' : 'bg-slate-200 text-slate-400'
                }`}>
                  {skillGapComplete ? <Check className="w-4 h-4 stroke-[3]" /> : <RefreshCw className="w-4 h-4 animate-spin text-[#1B5E20]" />}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <h4 className="font-bold text-sm text-slate-900">2. Skill Gap Analysis</h4>
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                      skillGapComplete ? 'bg-[#E8F5E9] text-[#1B5E20]' : 'bg-slate-200 text-slate-600'
                    }`}>
                      {skillGapComplete ? 'Completed' : 'In Progress'}
                    </span>
                  </div>
                  <p className="text-xs text-slate-500 mt-0.5 leading-relaxed">
                    Analyzing resume against target role ({targetRole}) to identify verified, partial, and missing skills.
                  </p>
                  {skillGapComplete && (
                    <span className="inline-block mt-2 text-[11px] font-semibold text-[#1B5E20] bg-white px-2 py-0.5 rounded border border-[#C8E6C9]">
                      ✓ 8 verified competencies identified
                    </span>
                  )}
                </div>
              </div>

              {/* Module 3 */}
              <div className={`p-4 rounded-2xl border transition-all flex items-start space-x-3 ${
                jdMatchingComplete ? 'bg-[#E8F5E9]/40 border-[#C8E6C9]' : 'bg-slate-50 border-slate-200'
              }`}>
                <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${
                  jdMatchingComplete ? 'bg-[#1B5E20] text-white' : 'bg-slate-200 text-slate-400'
                }`}>
                  {jdMatchingComplete ? <Check className="w-4 h-4 stroke-[3]" /> : <RefreshCw className="w-4 h-4 animate-spin text-[#1B5E20]" />}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <h4 className="font-bold text-sm text-slate-900">3. JD Matching</h4>
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                      jdMatchingComplete ? 'bg-[#E8F5E9] text-[#1B5E20]' : 'bg-slate-200 text-slate-600'
                    }`}>
                      {jdMatchingComplete ? 'Completed' : 'In Progress'}
                    </span>
                  </div>
                  <p className="text-xs text-slate-500 mt-0.5 leading-relaxed">
                    Comparing resume with target job description and generating relevant matching information.
                  </p>
                  {jdMatchingComplete && (
                    <span className="inline-block mt-2 text-[11px] font-semibold text-[#1B5E20] bg-white px-2 py-0.5 rounded border border-[#C8E6C9]">
                      ✓ Role Alignment: 92% Match
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Bottom Status */}
            <div className="pt-2 text-center">
              {analysisStep === 3 ? (
                <div className="p-3 bg-[#E8F5E9] text-[#1B5E20] font-bold text-xs rounded-xl flex items-center justify-center gap-2">
                  <CheckCircle2 className="w-4 h-4" /> All 3 modules complete! Opening ATS Dashboard...
                </div>
              ) : (
                <p className="text-xs text-slate-500">Real-time AI pipeline evaluating competencies...</p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* "WHY?" ENLARGED EXPLANATION MODAL WITH PURE BACKGROUND BLUR (NO COLOR TINT) */}
      {/* ========================================================================= */}
      {selectedWhyMetric && (
        <div 
          onClick={() => setSelectedWhyMetric(null)}
          className="fixed inset-0 z-50 flex items-center justify-center p-4 backdrop-blur-md bg-transparent animate-fade-in"
        >
          <div 
            onClick={(e) => e.stopPropagation()}
            className="bg-white w-full max-w-2xl max-h-[85vh] overflow-y-auto rounded-3xl p-6 sm:p-8 shadow-2xl border border-slate-200 space-y-6"
          >
            {/* Header */}
            <div className="flex items-center justify-between border-b border-slate-100 pb-4">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 rounded-xl bg-[#E8F5E9] text-[#1B5E20] flex items-center justify-center font-bold">
                  <Zap className="w-5 h-5 text-[#1B5E20]" />
                </div>
                <div>
                  <h3 className="font-bold text-lg text-slate-900">{selectedWhyMetric.metricName} Diagnostics</h3>
                  <p className="text-xs text-slate-500">Weight Contribution: {selectedWhyMetric.weightPercentage}% of Overall ATS Score</p>
                </div>
              </div>
              <button 
                onClick={() => setSelectedWhyMetric(null)}
                className="w-8 h-8 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 flex items-center justify-center transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Score & Formula Card */}
            <div className="bg-slate-50 rounded-2xl p-5 border border-slate-200 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Mathematical Calculation Formula</span>
                <span className="text-sm font-extrabold text-[#1B5E20]">{selectedWhyMetric.score} / {selectedWhyMetric.maxScore}</span>
              </div>
              <div className="p-3 bg-white rounded-xl border border-slate-200 font-mono text-xs text-slate-800 font-semibold">
                {selectedWhyMetric.formula}
              </div>
              <p className="text-xs text-slate-600 leading-relaxed">
                {selectedWhyMetric.formulaDescription}
              </p>
            </div>

            {/* Arithmetic Breakdown */}
            <div className="bg-[#E8F5E9] rounded-2xl p-5 border border-[#C8E6C9] space-y-2">
              <span className="text-xs font-bold uppercase tracking-wider text-[#1B5E20]">Arithmetic Execution</span>
              <p className="font-mono text-xs font-bold text-[#0D3B12] bg-white/80 p-3 rounded-xl border border-[#C8E6C9]">
                {selectedWhyMetric.arithmeticComputation}
              </p>
            </div>

            {/* Evidence Detected */}
            <div className="space-y-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">Detected Profile Evidence ({selectedWhyMetric.evidenceFound.length} items)</h4>
              <ul className="space-y-2">
                {selectedWhyMetric.evidenceFound.map((ev, i) => (
                  <li key={i} className="flex items-start gap-2.5 text-xs text-slate-700 bg-slate-50 p-2.5 rounded-xl border border-slate-100">
                    <CheckCircle2 className="w-4 h-4 text-[#1B5E20] shrink-0 mt-0.5" />
                    <span>{ev}</span>
                  </li>
                ))}
              </ul>
            </div>

            {/* Improvement Suggestion */}
            <div className="p-4 bg-amber-50 rounded-2xl border border-amber-200 text-xs text-amber-800 space-y-1">
              <span className="font-bold uppercase tracking-wider text-[10px] text-amber-900">Recommended Action to Maximize Score:</span>
              <p>{selectedWhyMetric.improvementSuggestion}</p>
            </div>

            {/* Close Button */}
            <div className="pt-2 flex justify-end">
              <button
                onClick={() => setSelectedWhyMetric(null)}
                className="px-5 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-semibold text-xs transition-colors cursor-pointer"
              >
                Close Diagnostics
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
