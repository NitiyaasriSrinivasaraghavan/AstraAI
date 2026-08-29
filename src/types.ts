export interface AtsCalculation {
  metricName: string;
  score: number;
  maxScore: number;
  weightPercentage: number;
  formula: string;
  formulaDescription: string;
  arithmeticComputation: string;
  evidenceFound: string[];
  penaltyDetails?: string;
  improvementSuggestion: string;
}

export interface AtsScoreResult {
  overallScore: number;
  candidateName: string;
  candidateEmail: string;
  candidatePhone: string;
  candidateLocation: string;
  targetRole: string;
  keywordCoverage: AtsCalculation;
  resumeStructure: AtsCalculation;
  formattingSafety: AtsCalculation;
  parsingAccuracy: AtsCalculation;
  strengths: string[];
  weaknesses: string[];
  priorityFixes: string[];
}

export interface SkillGapItem {
  name: string;
  category: 'Languages' | 'Frameworks' | 'Cloud & Tools' | 'Architecture' | 'Soft Skills';
  status: 'VERIFIED' | 'PARTIAL' | 'MISSING';
  matchPercent: number;
  evidence: string;
  actionItem: string;
}

export interface JdMatchItem {
  requirement: string;
  importance: 'HIGH' | 'MEDIUM' | 'NICE_TO_HAVE';
  matched: boolean;
  notes: string;
}
