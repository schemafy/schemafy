interface CursorLabelProps {
  name: string;
  color: string;
}

export const CursorLabel = ({ name, color }: CursorLabelProps) => (
  <div
    className="mt-0.5 px-1.5 py-0.5 rounded text-white whitespace-nowrap font-overline-xs"
    style={{ backgroundColor: color }}
  >
    {name}
  </div>
);
