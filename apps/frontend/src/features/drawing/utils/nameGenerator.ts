export const generateUniqueName = (existingNames: string[], prefix: string, suffix: string = ''): string => {
  if (existingNames.length === 0) {
    return `${prefix}1${suffix}`;
  }

  const pattern = new RegExp(`^${escapeRegExp(prefix)}(\\d+)${escapeRegExp(suffix)}$`);
  const numbers = existingNames
    .map((name) => {
      const match = name.match(pattern);
      return match ? parseInt(match[1], 10) : 0;
    })
    .filter((num) => !isNaN(num));

  const maxNumber = numbers.length > 0 ? Math.max(...numbers) : 0;
  return `${prefix}${maxNumber + 1}${suffix}`;
};

const escapeRegExp = (str: string): string => {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
};
