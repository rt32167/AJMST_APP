"""Read-only compatibility check for an AJMST.db backup."""

import argparse
import glob
import pathlib
import sqlite3


def expected_fields(config_path):
    tables = {}
    table = None
    for line in config_path.read_text(encoding='utf-8').splitlines():
        line = line.strip()
        if line.startswith('tableName='):
            table = line.partition('=')[2]
            tables[table] = set()
        elif line.startswith('fieldName=') and table:
            tables[table].add(line.partition('=')[2])
    return tables


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    backups = glob.glob(str(pathlib.Path(__file__).resolve().parents[2] / '*.db'))
    parser.add_argument('database', nargs='?', type=pathlib.Path,
                        default=pathlib.Path(backups[0]) if len(backups) == 1 else None)
    args = parser.parse_args()
    if args.database is None:
        parser.error('specify an AJMST.db file')
    config = pathlib.Path(__file__).resolve().parents[1] / 'res/raw/ormlite_config.txt'
    expected = expected_fields(config)
    con = sqlite3.connect(args.database.resolve().as_uri() + '?mode=ro', uri=True)
    con.execute('PRAGMA query_only=ON')
    version = con.execute('PRAGMA user_version').fetchone()[0]
    problems = []
    if version != 15:
        problems.append(f'user_version={version}, expected 15')
    for table, fields in expected.items():
        found = {row[1] for row in con.execute(f'PRAGMA table_info("{table}")')}
        if not found:
            problems.append(f'missing table: {table}')
        for field in sorted(fields - found):
            problems.append(f'missing column: {table}.{field}')
    con.close()
    if problems:
        print('\n'.join(problems))
        raise SystemExit(1)
    print(f'Compatible: user_version=15, {len(expected)} ORM tables and all configured columns present.')


if __name__ == '__main__':
    main()
