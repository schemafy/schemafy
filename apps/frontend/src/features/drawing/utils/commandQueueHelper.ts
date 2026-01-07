import { CommandQueue } from '../queue/CommandQueue';
import type { Command } from '../queue/Command';

export async function executeCommandWithValidation(
  command: Command,
  optimisticUpdate: () => void,
): Promise<void> {
  const commandQueue = CommandQueue.getInstance();

  try {
    optimisticUpdate();
    commandQueue.enqueue(command);
  } catch (error) {
    console.error(error);
    throw error;
  }
}
