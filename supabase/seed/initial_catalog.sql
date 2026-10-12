-- REVIEW-ONLY initial catalog seed. Do not run automatically or from application startup.
-- Safe to rerun: stable category slugs and product SKUs prevent duplicates. Existing
-- admin-managed product prices, stock, visibility, descriptions, and images are preserved.

begin;

insert into public.categories (name, slug, description, sort_order, is_visible)
values
  ('Fresh Vegetables', 'fresh-vegetables', 'Vegetables available from The Green Shop.', 10, true),
  ('Fresh Fruits', 'fresh-fruits', 'Fruits available from The Green Shop.', 20, true)
on conflict (slug) do nothing;

insert into public.products (category_id, sku, name, description, unit, package_size, price, compare_at_price, is_visible)
select c.id, seed.sku, seed.name, seed.description, seed.unit, seed.package_size,
       seed.price, seed.compare_at_price, true
from (values
  ('fresh-vegetables', 'TGS-VEG-TOMATO-500G', 'Tomato', 'Tomatoes sold in a 500 g pack.', 'pack', '500 g', 35.00::numeric, null::numeric),
  ('fresh-vegetables', 'TGS-VEG-ONION-1KG', 'Onion', 'Onions sold in a 1 kg pack.', 'pack', '1 kg', 48.00::numeric, null::numeric),
  ('fresh-vegetables', 'TGS-VEG-POTATO-1KG', 'Potato', 'Potatoes sold in a 1 kg pack.', 'pack', '1 kg', 42.00::numeric, null::numeric),
  ('fresh-fruits', 'TGS-FRUIT-BANANA-6PC', 'Banana', 'Bananas sold as a pack of 6.', 'pack', '6 pieces', 55.00::numeric, null::numeric),
  ('fresh-fruits', 'TGS-FRUIT-APPLE-4PC', 'Apple', 'Apples sold as a pack of 4.', 'pack', '4 pieces', 149.00::numeric, null::numeric)
) as seed(category_slug, sku, name, description, unit, package_size, price, compare_at_price)
join public.categories c on c.slug = seed.category_slug
on conflict (sku) do nothing;

insert into public.inventory (product_id, on_hand, reserved, low_stock_threshold)
select p.id, 20, 0, 5
from public.products p
where p.sku in (
  'TGS-VEG-TOMATO-500G', 'TGS-VEG-ONION-1KG', 'TGS-VEG-POTATO-1KG',
  'TGS-FRUIT-BANANA-6PC', 'TGS-FRUIT-APPLE-4PC'
)
on conflict (product_id) do nothing;

-- No promotion is inserted: discount terms and validity require an explicit business decision.

commit;
