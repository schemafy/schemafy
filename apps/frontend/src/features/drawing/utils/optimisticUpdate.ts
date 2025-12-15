export async function withOptimisticUpdate<T, R = void>(
  optimisticFunction: () => R,
  apiFunction: () => Promise<T>,
  rollbackFunction: (rollbackData: R) => void,
): Promise<T> {
  const apiPromise = apiFunction();
  const rollbackData = optimisticFunction();

  try {
    return await apiPromise;
  } catch (error) {
    rollbackFunction(rollbackData);
    throw error;
  }
}
