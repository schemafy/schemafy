import { useState, type DragEvent } from 'react';
import type { FieldType } from '../types';

interface UseDragAndDropProps {
  items: FieldType[];
  onReorder: (newItems: FieldType[]) => void;
}

interface UseDragAndDropReturn {
  draggedItem: string | null;
  dragOverItem: string | null;
  handleDragStart: (e: DragEvent, itemId: string) => void;
  handleDragOver: (e: DragEvent, itemId: string) => void;
  handleDragLeave: (e: DragEvent) => void;
  handleDrop: (e: DragEvent, dropTargetId: string) => void;
  handleDragEnd: () => void;
  isDragging: boolean;
  isDraggedItem: (itemId: string) => boolean;
  isDragOverItem: (itemId: string) => boolean;
}

export const useDragAndDrop = ({ items, onReorder }: UseDragAndDropProps): UseDragAndDropReturn => {
  const [draggedItem, setDraggedItem] = useState<string | null>(null);
  const [dragOverItem, setDragOverItem] = useState<string | null>(null);

  const handleDragStart = (e: DragEvent, itemId: string) => {
    e.stopPropagation();
    setDraggedItem(itemId);
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/html', itemId);
  };

  const handleDragOver = (e: DragEvent, itemId: string) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    setDragOverItem(itemId);
  };

  const handleDragLeave = (e: DragEvent) => {
    e.preventDefault();
    setDragOverItem(null);
  };

  const handleDrop = (e: DragEvent, dropTargetId: string) => {
    e.preventDefault();

    if (!draggedItem || draggedItem === dropTargetId) {
      setDraggedItem(null);
      setDragOverItem(null);
      return;
    }

    const draggedIndex = items.findIndex((item) => item.id === draggedItem);
    const targetIndex = items.findIndex((item) => item.id === dropTargetId);

    if (draggedIndex !== -1 && targetIndex !== -1) {
      const newItems = [...items];
      const [draggedElement] = newItems.splice(draggedIndex, 1);
      newItems.splice(targetIndex, 0, draggedElement);

      onReorder(newItems);
    }

    setDraggedItem(null);
    setDragOverItem(null);
  };

  const handleDragEnd = () => {
    setDraggedItem(null);
    setDragOverItem(null);
  };

  const isDragging = draggedItem !== null;

  const isDraggedItem = (itemId: string) => draggedItem === itemId;

  const isDragOverItem = (itemId: string) => dragOverItem === itemId;

  return {
    draggedItem,
    dragOverItem,
    handleDragStart,
    handleDragOver,
    handleDragLeave,
    handleDrop,
    handleDragEnd,
    isDragging,
    isDraggedItem,
    isDragOverItem,
  };
};
