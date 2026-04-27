#!/usr/bin/env python3
# Disclaimer
# Notice: Any sample scripts, code, or commands comes with the following notification.
#
# This Sample Code is provided for the purpose of illustration only and is not intended to be used in a production
# environment. THIS SAMPLE CODE AND ANY RELATED INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER
# EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
# PARTICULAR PURPOSE.
#
# We grant You a nonexclusive, royalty-free right to use and modify the Sample Code and to reproduce and distribute
# the object code form of the Sample Code, provided that You agree: (i) to not use Our name, logo, or trademarks to
# market Your software product in which the Sample Code is embedded; (ii) to include a valid copyright notice on Your
# software product in which the Sample Code is embedded; and (iii) to indemnify, hold harmless, and defend Us and Our
# suppliers from and against any claims or lawsuits, including attorneys' fees, that arise or result from the use or
# distribution of the Sample Code.
#
# Please note: None of the conditions outlined in the disclaimer above will supersede the terms and conditions
# contained within the Customers Support Services Description.
"""Generate random CSV test files for the Upload Quarkus PoC demo.

CSV format (upload CSV):
  cpf,nome,renda_mensal,cidade,uf,num_dependentes,data_nascimento

Usage:
  python generate_csv.py --rows 7500 --files 10 --output-dir tests/data
"""
from __future__ import annotations

import argparse
import csv
import os
import random
import string
from datetime import date, timedelta
from pathlib import Path

UF_LIST = ["SP", "RJ", "MG", "BA", "PR", "RS", "PE", "CE", "PA", "SC"]
CIDADES = [
    "Sao Paulo", "Rio de Janeiro", "Belo Horizonte", "Salvador",
    "Curitiba", "Porto Alegre", "Recife", "Fortaleza", "Belem",
    "Florianopolis", "Manaus", "Goiania", "Brasilia", "Campinas",
    "Guarulhos", "Osasco", "Santos", "Joinville", "Londrina", "Niteroi",
]
NOMES_PRIMEIRO = [
    "Ana", "Carlos", "Maria", "Jose", "Paula", "Pedro", "Fernanda",
    "Lucas", "Julia", "Rafael", "Beatriz", "Gabriel", "Larissa",
    "Mateus", "Camila", "Bruno", "Amanda", "Diego", "Leticia", "Thiago",
]
NOMES_SOBRENOME = [
    "Silva", "Santos", "Oliveira", "Souza", "Pereira", "Costa",
    "Rodrigues", "Almeida", "Nascimento", "Lima", "Araujo", "Fernandes",
    "Carvalho", "Gomes", "Martins", "Rocha", "Ribeiro", "Barbosa",
]


def _random_cpf() -> str:
    digits = [random.randint(0, 9) for _ in range(11)]
    return "".join(str(d) for d in digits)


def _random_nome() -> str:
    return f"{random.choice(NOMES_PRIMEIRO)} {random.choice(NOMES_SOBRENOME)}"


def _random_nascimento() -> str:
    start = date(1960, 1, 1)
    end = date(2005, 12, 31)
    delta = (end - start).days
    return (start + timedelta(days=random.randint(0, delta))).isoformat()


def generate_csv(path: Path, num_rows: int) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow([
            "cpf", "nome", "renda_mensal", "cidade", "uf",
            "num_dependentes", "data_nascimento",
        ])
        for _ in range(num_rows):
            writer.writerow([
                _random_cpf(),
                _random_nome(),
                f"{random.uniform(1000, 8000):.2f}",
                random.choice(CIDADES),
                random.choice(UF_LIST),
                random.randint(0, 6),
                _random_nascimento(),
            ])


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate test CSV files")
    parser.add_argument("--rows", type=int, default=7500, help="Rows per file (5000-10000)")
    parser.add_argument("--files", type=int, default=10, help="Number of files")
    parser.add_argument("--output-dir", type=str, default="tests/data")
    args = parser.parse_args()

    output = Path(args.output_dir)
    for i in range(1, args.files + 1):
        num_rows = random.randint(5000, 10000) if args.rows == 0 else args.rows
        path = output / f"sample_{i:02d}.csv"
        generate_csv(path, num_rows)
        print(f"  {path}  ({num_rows} rows)")

    print(f"\nGenerated {args.files} files in {output}/")


if __name__ == "__main__":
    main()
