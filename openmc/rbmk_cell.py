"""RBMK-1000 fuel-channel lattice cell in OpenMC.

This is the external "mechanics" side of the split Ben set: OpenMC computes the
real neutron physics; Minecraft only displays the result.

Geometry: one RBMK fuel channel on a square graphite lattice, modelled as an
infinite lattice of identical cells with reflective boundaries -- a standard
lattice-cell k-infinity calculation.

An RBMK channel does not hold a single rod. It holds an 18-rod fuel assembly
(two rings of 6 and 12 rods around a central carrier rod) inside an 80 mm
pressure tube, in a 250 mm pitch square graphite column. The rods are only
13.6 mm across, so resolving each one inside a 250 mm cell would need a very
fine mesh for no benefit at the cell level; the assembly is therefore
homogenised inside the pressure-tube bore, which is standard lattice-cell
practice. The coolant-void coefficient -- the point of this model -- depends on
the water/graphite/fuel volume fractions, which the homogenisation preserves.

Design values and where they come from:
  - lattice pitch 250 mm             : RBMK design (Dollezhal; widely documented)
  - fuel: 2.0% enriched UO2          : ChNPP Unit 4 fresh load (INSAG-7 / NUREG-1250)
  - 18 fuel rods per channel         : RBMK-1000 fuel assembly
  - fuel rod diameter 13.6 mm        : RBMK fuel element
  - pressure-tube bore 80 mm         : RBMK channel
  - graphite density 1.65 g/cm3      : reactor-grade graphite
  - coolant: light water             : RBMK coolant
"""
from __future__ import annotations

import openmc

# Design values, in cm.
PITCH = 25.0        # lattice pitch, cm (250 mm)
CELL_HALF = PITCH / 2.0
CHANNEL_R = 4.0     # pressure-tube inner radius, cm (80 mm bore)

# Assembly homogenised inside the bore. The 18 rods of 13.6 mm dia do not fill
# the 80 mm tube; the balance is coolant water. These volume fractions set the
# void worth and are an approximation of the real assembly geometry.
FUEL_VF = 0.62      # fuel volume fraction inside the pressure-tube bore
CLAD_VF = 0.12      # cladding volume fraction inside the bore

COOLANT_DENSITY_FULL = 0.65  # g/cm3 at RBMK operating conditions


def materials(coolant_density: float = COOLANT_DENSITY_FULL) -> dict[str, openmc.Material]:
    fuel = openmc.Material(name="UO2 fuel (2.0% enriched)")
    fuel.add_element("U", 1.0, "ao", enrichment=2.0)
    fuel.add_element("O", 2.0, "ao")
    fuel.set_density("g/cm3", 10.4)

    clad = openmc.Material(name="Zr-1%Nb cladding")
    clad.add_element("Zr", 0.99, "wo")
    clad.add_element("Nb", 0.01, "wo")
    clad.set_density("g/cm3", 6.55)

    graphite = openmc.Material(name="graphite moderator")
    graphite.add_element("C", 1.0, "ao")
    graphite.set_density("g/cm3", 1.65)
    graphite.add_s_alpha_beta("c_Graphite")

    water = openmc.Material(name="light-water coolant")
    water.add_element("H", 2.0, "ao")
    water.add_element("O", 1.0, "ao")
    water.set_density("g/cm3", coolant_density)

    return {"fuel": fuel, "clad": clad, "graphite": graphite, "water": water}


def channel_material(mats: dict[str, openmc.Material], voided: bool) -> openmc.Material:
    """Homogenise fuel + cladding (+ coolant) into the material filling the bore.

    voided=True removes the coolant water -- a full steam void, the condition the
    void coefficient is defined against."""
    if voided:
        w = FUEL_VF + CLAD_VF
        return openmc.Material.mix_materials(
            [mats["fuel"], mats["clad"]], [FUEL_VF / w, CLAD_VF / w], "vo",
            name="channel contents (voided)",
        )
    mix = openmc.Material.mix_materials(
        [mats["fuel"], mats["clad"], mats["water"]],
        [FUEL_VF, CLAD_VF, 1.0 - FUEL_VF - CLAD_VF], "vo",
        name="channel contents (homogenised)",
    )
    mix.add_s_alpha_beta("c_H_in_H2O")
    return mix


def build_model(coolant_density: float = COOLANT_DENSITY_FULL) -> openmc.Model:
    """Build the RBMK lattice cell at the given coolant density (0 = void)."""
    voided = coolant_density <= 0.0
    mats = materials(coolant_density if not voided else COOLANT_DENSITY_FULL)
    channel = channel_material(mats, voided)

    chan_or = openmc.ZCylinder(r=CHANNEL_R)
    x_lo = openmc.XPlane(-CELL_HALF, boundary_type="reflective")
    x_hi = openmc.XPlane(CELL_HALF, boundary_type="reflective")
    y_lo = openmc.YPlane(-CELL_HALF, boundary_type="reflective")
    y_hi = openmc.YPlane(CELL_HALF, boundary_type="reflective")
    square = +x_lo & -x_hi & +y_lo & -y_hi

    universe = openmc.Universe(cells=[
        openmc.Cell(name="channel", fill=channel, region=-chan_or),
        openmc.Cell(name="graphite", fill=mats["graphite"], region=+chan_or & square),
    ])
    geometry = openmc.Geometry(universe)

    settings = openmc.Settings()
    settings.batches = 120
    settings.inactive = 30
    settings.particles = 8000
    settings.run_mode = "eigenvalue"
    settings.source = openmc.IndependentSource(space=openmc.stats.Point((0, 0, 0)))
    settings.temperature = {"default": 293.6}

    return openmc.Model(geometry, openmc.Materials([mats["graphite"], channel]), settings)


def main() -> None:
    build_model().export_to_xml(".")
    print("wrote geometry.xml / materials.xml / settings.xml for the RBMK lattice cell")


if __name__ == "__main__":
    main()
