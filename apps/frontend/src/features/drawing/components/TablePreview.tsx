interface TablePreviewProps {
  mousePosition: { x: number; y: number };
}

export const TablePreview = ({ mousePosition }: TablePreviewProps) => {
  return (
    <div
      className="bg-schemafy-button-bg w-[200px] h-[100px] rounded-lg"
      style={{
        position: 'absolute',
        pointerEvents: 'none',
        zIndex: 999,
        opacity: 0.4,
        transform: `translate(${mousePosition.x}px, ${mousePosition.y - 50}px)`,
      }}
    />
  );
};
