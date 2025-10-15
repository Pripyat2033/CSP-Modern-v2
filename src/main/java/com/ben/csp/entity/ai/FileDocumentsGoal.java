package com.ben.csp.entity.ai;

import com.ben.csp.block.entity.ArchiveFilingCabinetBlockEntity;
import com.ben.csp.block.ModBlocks;
import com.ben.csp.entity.ProrabEntity;
import com.ben.csp.entity.UchetchikEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Science Grade: AI Goal for an Uchetchik to collect reports from a Prorab
 * and file them in an Archive Filing Cabinet.
 */
public class FileDocumentsGoal extends Goal {

    private enum State {
        IDLE,
        GOING_TO_PRORAB,
        GOING_TO_CABINET
    }

    private final UchetchikEntity uchetchik;
    private ProrabEntity targetProrab;
    private BlockPos targetCabinetPos;
    private State currentState = State.IDLE;

    public FileDocumentsGoal(UchetchikEntity uchetchik) {
        this.uchetchik = uchetchik;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (this.uchetchik.isCarryingDocuments()) {
            // If already carrying documents, we need to find a cabinet.
            return findFilingCabinet().isPresent();
        } else {
            // Otherwise, find a Prorab with pending reports.
            return findProrabWithReports().isPresent();
        }
    }

    @Override
    public boolean shouldContinue() {
        return this.currentState != State.IDLE;
    }

    @Override
    public void start() {
        if (this.uchetchik.isCarryingDocuments()) {
            findFilingCabinet().ifPresent(pos -> {
                this.targetCabinetPos = pos;
                this.uchetchik.getNavigation().startMovingTo(pos.getX(), pos.getY(), pos.getZ(), 1.0D);
                this.currentState = State.GOING_TO_CABINET;
            });
        } else {
            findProrabWithReports().ifPresent(prorab -> {
                this.targetProrab = prorab;
                this.uchetchik.getNavigation().startMovingTo(prorab, 1.0D);
                this.currentState = State.GOING_TO_PRORAB;
            });
        }
    }

    @Override
    public void tick() {
        if (currentState == State.GOING_TO_PRORAB && targetProrab != null && this.uchetchik.distanceTo(targetProrab) < 3.0) {
            // Arrived at Prorab, collect documents.
            NbtCompound report = targetProrab.getNextReport();
            if (report != null) {
                this.uchetchik.setCarriedDocument(report);
            }
            this.currentState = State.IDLE; // Goal will restart to find a cabinet.
        } else if (currentState == State.GOING_TO_CABINET && targetCabinetPos != null && this.uchetchik.getBlockPos().isWithinDistance(targetCabinetPos, 3.0)) {
            // Arrived at cabinet, file document.
            BlockEntity be = this.uchetchik.getWorld().getBlockEntity(targetCabinetPos);
            if (be instanceof ArchiveFilingCabinetBlockEntity cabinet) {
                cabinet.fileDocument(this.uchetchik.getCarriedDocument());
                this.uchetchik.setCarriedDocument(null);
            }
            this.currentState = State.IDLE;
        }
    }

    @Override
    public void stop() {
        this.uchetchik.getNavigation().stop();
        this.currentState = State.IDLE;
        this.targetProrab = null;
        this.targetCabinetPos = null;
    }

    private Optional<ProrabEntity> findProrabWithReports() {
        return this.uchetchik.getWorld().getEntitiesByClass(ProrabEntity.class, this.uchetchik.getBoundingBox().expand(32), 
                prorab -> prorab.hasPendingReports() && this.uchetchik.getEnterpriseName().equals(prorab.getEnterpriseName())).stream().findFirst();
    }

    private Optional<BlockPos> findFilingCabinet() {
        return BlockPos.findClosest(this.uchetchik.getBlockPos(), 16, 8,
                pos -> this.uchetchik.getWorld().getBlockState(pos).isOf(ModBlocks.ARCHIVE_FILING_CABINET_BLOCK));
    }
}