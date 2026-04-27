import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { filter } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

@Component({
  selector: 'app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatIconModule, MatTooltipModule],
})
export class ShellComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly sidebarCollapsed = signal(false);
  protected readonly pageTitle = signal('Dashboard');

  protected readonly navGroups: NavGroup[] = [
    {
      label: 'Principal',
      items: [
        { label: 'Dashboard',       icon: 'dashboard',      route: '/dashboard' },
        { label: 'Clientes',        icon: 'people',         route: '/clientes' },
        { label: 'Veículos',        icon: 'directions_car', route: '/veiculos' },
      ],
    },
    {
      label: 'Operações',
      items: [
        { label: 'Peças e Insumos',   icon: 'inventory_2',   route: '/pecas' },
        { label: 'Serviços',          icon: 'handyman',      route: '/servicos' },
        { label: 'Ordens de Serviço', icon: 'assignment',    route: '/ordens' },
        { label: 'Pedidos de Compra', icon: 'shopping_cart', route: '/pedidos-compra' },
      ],
    },
    {
      label: 'Acesso Rápido',
      items: [
        { label: 'Fila do Mecânico',  icon: 'build',         route: '/mecanico' },
        { label: 'Portal do Cliente', icon: 'person_search',  route: '/acompanhar' },
      ],
    },
  ];

  private readonly allItems = this.navGroups.flatMap(g => g.items);

  constructor() {
    // Update page title on every navigation
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(e => {
        const url = (e as NavigationEnd).urlAfterRedirects;
        const item = this.allItems.find(n => url.startsWith(n.route));
        this.pageTitle.set(item?.label ?? 'SIASE');
      });
  }
}
