export async function withOptimisticUpdate<T, R = void>(
  optimisticFunction: () => R,
  apiFunction: () => Promise<T>,
  rollbackFunction: (rollbackData: R) => void,
): Promise<T> {
  const rollbackData = optimisticFunction();
  try {
    return await apiFunction();
  } catch (error) {
    rollbackFunction(rollbackData);
    throw error;
  }
}
