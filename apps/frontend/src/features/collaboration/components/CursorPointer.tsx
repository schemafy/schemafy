import { CURSOR_POINTER_SIZE } from '@/features/collaboration/utils';

interface CursorPointerProps {
  color: string;
}

export const CursorPointer = ({color}: CursorPointerProps) => (
  <svg
    width={CURSOR_POINTER_SIZE.width}
    height={CURSOR_POINTER_SIZE.height}
    viewBox={`0 0 ${CURSOR_POINTER_SIZE.width} ${CURSOR_POINTER_SIZE.height}`}
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      d="M0 0 L0 18 L4.5 14 L7 20 L9.5 19 L7 13 L13 13 Z"
      fill={color}
      stroke="white"
      strokeWidth="1.5"
      strokeLinejoin="round"
    />
  </svg>
);
