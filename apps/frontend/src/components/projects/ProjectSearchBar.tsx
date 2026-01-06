interface ProjectSearchBarProps {
  value: string;
  onChange: (value: string) => void;
}

export const ProjectSearchBar = ({
  value,
  onChange,
}: ProjectSearchBarProps) => {
  return (
    <div className="mb-8">
      <div className="relative">
        <svg
          width="20"
          height="20"
          viewBox="0 0 20 20"
          fill="none"
          className="absolute left-4 top-1/2 -translate-y-1/2 text-schemafy-dark-gray"
        >
          <circle cx="9" cy="9" r="7" stroke="currentColor" strokeWidth="2" />
          <path
            d="M14 14L18 18"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          />
        </svg>
        <input
          type="text"
          placeholder="Search projects"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="w-full pl-12 pr-4 py-3 bg-schemafy-secondary rounded-xl font-body-md text-schemafy-text placeholder-schemafy-dark-gray focus:outline-none focus:ring-2 focus:ring-schemafy-button-bg"
        />
      </div>
    </div>
  );
};
