import React from "react";

interface LogoProps {
  size?: number;
  className?: string;
  showSubtitle?: boolean;
}

/**
 * Official NoviQ App Icon Tile
 * Dark green squircle with mint upward looping ribbon "N", top-right arrow tip, and 4-point sparkle star.
 */
export const NoviQLogoTile: React.FC<{ size?: number; className?: string }> = ({
  size = 64,
  className = "",
}) => {
  return (
    <div
      className={`relative flex items-center justify-center rounded-[28%] bg-gradient-to-b from-[#2C4A3E] to-[#1E352B] shadow-md ${className}`}
      style={{ width: size, height: size }}
    >
      <svg
        viewBox="0 0 100 100"
        className="w-full h-full p-1"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          <linearGradient id="noviqRibbonGrad" x1="20" y1="20" x2="80" y2="80" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#D8EAD9" />
            <stop offset="40%" stopColor="#B2D3B8" />
            <stop offset="80%" stopColor="#7CA786" />
            <stop offset="100%" stopColor="#D8EAD9" />
          </linearGradient>
          <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="1" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
          </filter>
        </defs>

        {/* 4-point Sparkle Star in top right corner */}
        <path
          d="M 68 8 Q 68 16 76 16 Q 68 16 68 24 Q 68 16 60 16 Q 68 16 68 8 Z"
          fill="#E8F7EB"
          filter="url(#glow)"
        />

        {/* 3D Ribbon "N" with Arrow Tip */}
        <path
          d="M 23 76 C 20 40 28 18 46 18 C 58 18 58 32 52 48 C 46 64 52 80 66 78 C 75 75 76 45 76 34"
          stroke="url(#noviqRibbonGrad)"
          strokeWidth="16"
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        {/* Left inner highlight pillar */}
        <path
          d="M 31 75 C 29 45 33 26 45 25"
          stroke="#EAF5EC"
          strokeWidth="11"
          strokeLinecap="round"
        />

        {/* Arrow Head pointing to top-right */}
        <polygon
          points="76,22 64,30 72,29 73,37 81,36 80,28 88,29"
          fill="#D8EAD9"
        />
      </svg>
    </div>
  );
};

/**
 * Official NoviQ Full Logo with App Icon, Typography, Tagline, and Flourish
 */
export const NoviQFullLogo: React.FC<LogoProps> = ({
  size = 96,
  className = "",
  showSubtitle = true,
}) => {
  return (
    <div className={`flex flex-col items-center justify-center text-center ${className}`}>
      <NoviQLogoTile size={size} />

      <div className="mt-4 flex items-center justify-center font-extrabold tracking-tight text-[#2C4A3E]">
        <span style={{ fontSize: `${Math.round(size * 0.38)}px` }}>Novi</span>
        <span style={{ fontSize: `${Math.round(size * 0.38)}px`, color: "#568367" }}>Q</span>
      </div>

      {showSubtitle && (
        <div className="mt-1 flex flex-col items-center">
          <span className="text-[11px] font-semibold tracking-[0.24em] text-[#335345] uppercase">
            Your AI Career Companion
          </span>
          <div className="mt-2 flex items-center w-36 opacity-75">
            <div className="flex-1 h-[1.5px] bg-gradient-to-r from-transparent to-[#88A790]" />
            <svg className="w-2.5 h-2.5 mx-1.5 text-[#88A790] fill-current" viewBox="0 0 16 16">
              <path d="M 8 0 Q 8 8 16 8 Q 8 8 8 16 Q 8 8 0 8 Q 8 8 8 0 Z" />
            </svg>
            <div className="flex-1 h-[1.5px] bg-gradient-to-l from-transparent to-[#88A790]" />
          </div>
        </div>
      )}
    </div>
  );
};

/**
 * Official Nova AI Chatbot Avatar / Logo Tile
 * Soft sage squircle with friendly robot avatar, smiling eyes, headphones, leaf antenna, speech bubble, and chest star.
 */
export const NovaAvatar: React.FC<{ size?: number; className?: string; showBorder?: boolean }> = ({
  size = 64,
  className = "",
  showBorder = true,
}) => {
  return (
    <div
      className={`relative flex items-center justify-center rounded-[28%] bg-[#D3E2D0] shadow-sm ${
        showBorder ? "border-[2.5px] border-[#476956]" : ""
      } ${className}`}
      style={{ width: size, height: size }}
    >
      <svg
        viewBox="0 0 100 100"
        className="w-full h-full p-1"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        {/* Leaf/Sprout Antenna */}
        <path
          d="M 50 24 C 50 12 62 8 66 10 C 64 20 55 24 50 24 Z"
          fill="#5D846D"
        />
        <line x1="50" y1="24" x2="51" y2="21" stroke="#476956" strokeWidth="3.5" strokeLinecap="round" />

        {/* Speech Bubble with 3 dots */}
        <rect x="62" y="14" width="26" height="17" rx="8" fill="#476956" />
        <polygon points="66,31 60,36 72,31" fill="#476956" />
        <circle cx="70" cy="22.5" r="1.5" fill="#FFFFFF" />
        <circle cx="75" cy="22.5" r="1.5" fill="#FFFFFF" />
        <circle cx="80" cy="22.5" r="1.5" fill="#FFFFFF" />

        {/* Torso & Shoulder Base */}
        <path
          d="M 24 94 C 26 70 74 70 76 94 Z"
          fill="#F3F8F2"
          stroke="#476956"
          strokeWidth="2"
        />
        {/* Chest Star Badge */}
        <circle cx="50" cy="79" r="8.5" fill="#476956" />
        <path
          d="M 50 74 Q 50 79 55 79 Q 50 79 50 84 Q 50 79 45 79 Q 50 79 50 74 Z"
          fill="#D4E7D6"
        />

        {/* Headphones / Earcups & Arch */}
        <path
          d="M 22 46 A 28 20 0 0 1 78 46"
          stroke="#476956"
          strokeWidth="3.5"
          fill="none"
        />
        <circle cx="20" cy="46" r="8" fill="#476956" />
        <circle cx="80" cy="46" r="8" fill="#476956" />

        {/* Robot Head Shell */}
        <rect
          x="22"
          y="26"
          width="56"
          height="44"
          rx="20"
          fill="#F3F8F2"
          stroke="#476956"
          strokeWidth="2"
        />

        {/* Visor Screen */}
        <rect x="28" y="33" width="44" height="29" rx="14" fill="#1D3328" />

        {/* Happy Smiling Eyes (^ ^) */}
        <path
          d="M 37 49 Q 41 42 45 49"
          stroke="#D4E7D6"
          strokeWidth="3.8"
          strokeLinecap="round"
          fill="none"
        />
        <path
          d="M 55 49 Q 59 42 63 49"
          stroke="#D4E7D6"
          strokeWidth="3.8"
          strokeLinecap="round"
          fill="none"
        />
      </svg>
    </div>
  );
};

/**
 * Official Nova Full Logo with Chatbot Avatar, Typography, Tagline, and Flourish
 */
export const NovaFullLogo: React.FC<LogoProps> = ({
  size = 96,
  className = "",
  showSubtitle = true,
}) => {
  return (
    <div className={`flex flex-col items-center justify-center text-center ${className}`}>
      <NovaAvatar size={size} />

      <div className="mt-4 flex items-center justify-center font-extrabold tracking-tight text-[#2B483B]">
        <span style={{ fontSize: `${Math.round(size * 0.38)}px` }}>Nov</span>
        <span style={{ fontSize: `${Math.round(size * 0.38)}px`, color: "#4E775F" }}>a</span>
      </div>

      {showSubtitle && (
        <div className="mt-1 flex flex-col items-center">
          <span className="text-[11px] font-semibold tracking-[0.24em] text-[#335345] uppercase">
            Your AI Career Assistant
          </span>
          <div className="mt-2 flex items-center w-36 opacity-75">
            <div className="flex-1 h-[1.5px] bg-gradient-to-r from-transparent to-[#88A790]" />
            <svg className="w-2.5 h-2.5 mx-1.5 text-[#88A790] fill-current" viewBox="0 0 16 16">
              <path d="M 8 0 Q 8 8 16 8 Q 8 8 8 16 Q 8 8 0 8 Q 8 8 8 0 Z" />
            </svg>
            <div className="flex-1 h-[1.5px] bg-gradient-to-l from-transparent to-[#88A790]" />
          </div>
        </div>
      )}
    </div>
  );
};
