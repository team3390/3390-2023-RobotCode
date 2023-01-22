package com.team3390.robot.subsystems;

import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.wpilibj2.command.SubsystemBase;

import com.team3390.lib.control.DriveController;
import com.team3390.lib.drivers.LazyTalonSRX;
import com.team3390.lib.drivers.TalonSRXCreator;
import com.team3390.robot.Constants;
import com.team3390.robot.Constants.LOWPOWERMODE_INCREASE_TYPE;
import com.team3390.robot.utility.CompetitionShuffleboard;
import com.team3390.robot.utility.LowPowerMode;
import com.team3390.robot.utility.PID;

public class DriveSubsystem extends SubsystemBase {
  
  private final CompetitionShuffleboard shuffleboard = CompetitionShuffleboard.INSTANCE;

  private boolean isBreakMode;

  private final LazyTalonSRX leftMaster, rightMaster, leftSlave, rightSlave;
  private final DriveController driveController;

  private final AHRS navX = new AHRS(Constants.SENSOR_NAVX_PORT);
  private final PID balancePID = new PID(
    Constants.DRIVE_BALANCE_PID_KP,
    Constants.DRIVE_BALANCE_PID_KI,
    Constants.DRIVE_BALANCE_PID_KD,
    Constants.DRIVE_BALANCE_PID_TOLERANCE,
    Constants.DRIVE_BALANCE_PID_MAXOUT,
    Constants.DRIVE_BALANCE_PID_MINOUT
  );

  public DriveSubsystem() {    
    leftMaster = TalonSRXCreator.createDefaultMasterTalon(Constants.DRIVE_LEFT_MASTER_ID);
    leftSlave = TalonSRXCreator.createDefaultPermanentSlaveTalon(Constants.DRIVE_LEFT_SLAVE_ID, Constants.DRIVE_LEFT_MASTER_ID);
    rightMaster = TalonSRXCreator.createDefaultMasterTalon(Constants.DRIVE_RIGHT_MASTER_ID);
    rightSlave = TalonSRXCreator.createDefaultPermanentSlaveTalon(Constants.DRIVE_RIGHT_SLAVE_ID, Constants.DRIVE_RIGHT_MASTER_ID);

    isBreakMode = true;

    driveController = new DriveController(leftMaster, rightMaster);

    balancePID.setSetpoint(0);
  }

  @Override
  public void periodic() {
    shuffleboard.robotBalancedEntry.setBoolean(balancePID.atSetpoint());
    shuffleboard.robotLowPowerModeEntry.setBoolean(LowPowerMode.INSTANCE.getLowDriveModeEnabled());
    shuffleboard.robotPitch.setDouble(Math.floor(navX.getRoll()) + Constants.DRIVE_NAVX_ROLL_DEADBAND);
    shuffleboard.robotBalancePIDOutput.setDouble(balancePID.output(balancePID.calculate(Math.floor(navX.getRoll()) + Constants.DRIVE_NAVX_ROLL_DEADBAND, 0)));
  }

  /**
   * Bütün sürüş ile alakalı sensörleri sıfırlar
   */
  public void resetSensors() {
    navX.reset();
  }

  /**
   * Bütün sürüş ile alakalı sensörleri kalibre eder
   * 
   * Robot ilk açıldığında kalibrasyon yapmak önemlidir!
   */
  public void calibrateSensors() {
    navX.calibrate();
  }

  /**
   * Bütün motorları durdurur.
   */
  public void stopMotors() {
    driveController.stopMotors();
  }

  public boolean isBreakMode() {
    return isBreakMode;
  }

  public void setBrakeMode(boolean shouldEnable) {
    if (isBreakMode != shouldEnable) {
      isBreakMode = shouldEnable;
      NeutralMode mode = shouldEnable ? NeutralMode.Brake : NeutralMode.Coast;

      leftMaster.setNeutralMode(mode);
      leftSlave.setNeutralMode(mode);

      rightMaster.setNeutralMode(mode);
      rightSlave.setNeutralMode(mode);
    }
  }

  /**
   * Robotun arcade tarzda sürülmesini sağlar
   * @param fwd X ekseninde hız (%)
   * @param rot Y ekseninde hız (%)
   */
  public void arcadeDrive(double fwd, double rot) {
    if (LowPowerMode.INSTANCE.getLowDriveModeEnabled()) {
      fwd = LowPowerMode.INSTANCE.calculate(fwd, LOWPOWERMODE_INCREASE_TYPE.TREE);
      rot = LowPowerMode.INSTANCE.calculate(rot, LOWPOWERMODE_INCREASE_TYPE.TREE);
    }
    driveController.arcadeDrive(fwd, rot, false);
  }

  /**
   * Robotun tank tarzında sürülmesini sağlar
   * @param leftPercent Sol tekereklere gidecek hız (%)
   * @param rightPercent Sağ tekereklere gidecek hız (%)
   */
  public void tankDrivePercent(double leftPercent, double rightPercent) {
    if (LowPowerMode.INSTANCE.getLowDriveModeEnabled()) {
      leftPercent = LowPowerMode.INSTANCE.calculate(leftPercent, LOWPOWERMODE_INCREASE_TYPE.TREE);
      rightPercent = LowPowerMode.INSTANCE.calculate(rightPercent, LOWPOWERMODE_INCREASE_TYPE.TREE);
    }
    driveController.tankDrive(leftPercent, rightPercent, false);
  }

  /**
   * Robotun rampanın üzerinde dengede durabilmesi için hazırlanmış
   * PID kontrolcüsü eşliğinde çalışan hizzalama bölümü
   */
  public void balanceRobot() {
    if (!balancePID.atSetpoint()) {
      double roll = Math.floor(navX.getRoll()) + Constants.DRIVE_NAVX_ROLL_DEADBAND;
      double calculatedSpeed = balancePID.output(balancePID.calculate(roll, 0));
      driveController.arcadeDrive(calculatedSpeed, 0, false);
    } else {
      driveController.stopMotors();
    }
  }
}