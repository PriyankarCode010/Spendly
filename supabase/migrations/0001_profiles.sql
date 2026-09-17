-- Phase 1 foundation: single per-user profile row.
-- Establishes the deny-by-default + own-row RLS pattern every future table follows.

create table if not exists public.profiles (
    id uuid primary key references auth.users (id) on delete cascade,
    currency text not null default 'INR',
    monthly_income numeric not null default 0,
    current_balance numeric not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table public.profiles enable row level security;

create policy "Users can select their own profile"
    on public.profiles for select
    to authenticated
    using ((select auth.uid()) = id);

create policy "Users can insert their own profile"
    on public.profiles for insert
    to authenticated
    with check ((select auth.uid()) = id);

create policy "Users can update their own profile"
    on public.profiles for update
    to authenticated
    using ((select auth.uid()) = id)
    with check ((select auth.uid()) = id);
