package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.can.WPI_TalonSRX;
import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.utility.PID;

public class DriveSubsystem extends SubsystemBase {
  
  private final WPI_TalonSRX leftLeader  = new WPI_TalonSRX(Constants.DRIVE_LEFT_LEADER_ID);
  private final WPI_TalonSRX leftMotor2  = new WPI_TalonSRX(Constants.DRIVE_LEFT_MOTOR2_ID);
  private final WPI_TalonSRX rightLeader = new WPI_TalonSRX(Constants.DRIVE_RIGHT_LEADER_ID);
  private final WPI_TalonSRX rightMotor2 = new WPI_TalonSRX(Constants.DRIVE_RIGHT_MOTOR2_ID);

  private final DifferentialDrive drive = new DifferentialDrive(leftLeader, rightLeader);

  private final AHRS navX = new AHRS(Constants.SENSOR_NAVX_PORT);
  private final PID balancePID = new PID(Constants.DRIVE_BALANCE_PID_KP, Constants.DRIVE_BALANCE_PID_KI, Constants.DRIVE_BALANCE_PID_KD, Constants.DRIVE_BALANCE_PID_TOLERANCE, Constants.DRIVE_BALANCE_PID_MAXOUT, Constants.DRIVE_BALANCE_PID_MINOUT);

  public DriveSubsystem() {
    leftLeader.setInverted(Constants.DRIVE_LEFT_INVERTED);
    leftMotor2.setInverted(Constants.DRIVE_LEFT_INVERTED);
    rightLeader.setInverted(Constants.DRIVE_RIGHT_INVERTED);
    rightMotor2.setInverted(Constants.DRIVE_RIGHT_INVERTED);

    leftMotor2.follow(leftLeader);
    rightMotor2.follow(rightLeader);

    /**
     * Robot ilk açıldığında kalibrasyon yapmak önemlidir!
     */
    navX.calibrate();
    resetSensors();

    balancePID.setSetpoint(0);
  }

  @Override
  public void periodic() {
    SmartDashboard.putBoolean("Balanced", balancePID.atSetpoint());    
  }

  /**
   * Bütün sürüş ile alakalı sensörleri sıfırlar
   */
  public void resetSensors() {
    navX.reset();
  }

  /**
   * Bütün motorları durdurur.
   */
  public void stopMotors() {
    drive.stopMotor();
  }

  /**
   * Robotun arcade tarzda sürülmesini sağlar
   * @param fwd X ekseninde hız (%)
   * @param rot Y ekseninde hız (%)
   */
  public void arcadeDrive(double fwd, double rot) {
    drive.arcadeDrive(fwd, rot);
  }

  /**
   * Robotun tank tarzında sürülmesini sağlar
   * @param leftPercent Sol tekereklere gidecek hız (%)
   * @param rightPercent Sağ tekereklere gidecek hız (%)
   */
  public void tankDrivePercent(double leftPercent, double rightPercent) {
    drive.tankDrive(leftPercent, rightPercent);
  }

  public void balanceRobot() {
    if (!balancePID.atSetpoint()) {
      double pitch = navX.getPitch();
      double calculatedSpeed = balancePID.output(balancePID.calculate(pitch, 0));
      drive.arcadeDrive(calculatedSpeed, 0);
    } else {
      drive.stopMotor();
    }
  }
}