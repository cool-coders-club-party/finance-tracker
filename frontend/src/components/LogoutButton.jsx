import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth.js';
import { Button } from './ui/button';

export default function LogoutButton() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  function handleClick() {
    logout(); // clears the session at once; the server request finishes in the background
    navigate('/login', { replace: true });
  }

  return (
    <Button type="button" variant="outline" onClick={handleClick}>
      Log out
    </Button>
  );
}