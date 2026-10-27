export function formatEnumLabel(value) {
  return value.toLowerCase().replaceAll('_', ' ').replace(/^./, (letter) => letter.toUpperCase());
}

export function validateMetadata(data) {
  const validTypes = Array.isArray(data?.types) && data.types.length > 0
    && data.types.every((type) => typeof type === 'string' && type.length > 0)
    && new Set(data.types).size === data.types.length;
  const validCategories = validTypes && Array.isArray(data.categories) && data.categories.length > 0
    && data.categories.every((category) => typeof category?.name === 'string' && category.name.length > 0
      && Array.isArray(category.allowedTypes) && category.allowedTypes.length > 0
      && category.allowedTypes.every((type) => data.types.includes(type)))
    && new Set(data.categories.map(({ name }) => name)).size === data.categories.length;
  if (!validCategories) throw new Error('Transaction options could not be read. Please try again.');
  return data;
}

export function categoriesForType(metadata, type) {
  return metadata.categories.filter((category) => category.allowedTypes.includes(type));
}

export function categoryAfterTypeChange(metadata, categoryName, nextType) {
  return categoriesForType(metadata, nextType).some(({ name }) => name === categoryName)
    ? categoryName : '';
}
